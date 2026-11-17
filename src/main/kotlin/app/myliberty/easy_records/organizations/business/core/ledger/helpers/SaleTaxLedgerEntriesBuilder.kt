package app.myliberty.easy_records.organizations.business.ledger.helpers

import app.myliberty.easy_records.locations.business.tax_entry.api.TaxEntryDto
import app.myliberty.easy_records.locations.business.tax_entry.api.TaxEntryService
import app.myliberty.easy_records.locations.business.tax_entry.api.TaxSourceType
import app.myliberty.easy_records.organizations.business.account.api.EntryType
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleTaxLedgerEntriesBuilder(
    private val taxEntryService: TaxEntryService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher
) {

    fun buildTransactionLevelEntries(saleReferenceNumber: String): List<LedgerEntryRequest> {
        val entries = taxEntryService.findBySourceReference(saleReferenceNumber, TaxSourceType.SALE)
        val payableAccountCodesByTaxTypeId = payableAccountCodesByTaxTypeId()
        return entries.flatMap { entry -> journalFor(entry, payableAccountCodesByTaxTypeId, reversal = false) }
    }

    fun buildTransactionLevelReversalEntries(saleReferenceNumber: String): List<LedgerEntryRequest> {
        val entries = taxEntryService.findBySourceReference(saleReferenceNumber, TaxSourceType.SALE_VOID)
        val payableAccountCodesByTaxTypeId = payableAccountCodesByTaxTypeId()
        return entries.flatMap { entry -> journalFor(entry, payableAccountCodesByTaxTypeId, reversal = true) }
    }

    private fun journalFor(
        taxEntryDto: TaxEntryDto,
        payableAccountCodesByTaxTypeId: Map<UUID, String>,
        reversal: Boolean
    ): List<LedgerEntryRequest> {
        val taxAmount = taxEntryDto.taxAmount.abs()
        val payableAccountCode = payableAccountCodesByTaxTypeId[taxEntryDto.taxTypeId]
            ?: throwMissingPayableAccountCodeException(taxEntryDto)

        return when {
            taxEntryDto.taxIsBilledToCustomerSeparately ->
                payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal)

            taxEntryDto.taxIsIncludedInTaxableAmount ->
                grossSalesLeg(taxAmount, debitingGrossSales = !reversal) +
                    payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal)

            else ->
                taxExpenseLeg(taxAmount, debitingExpense = !reversal) +
                    payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal)
        }
    }

    private fun payableLeg(payableAccountCode: String, taxAmount: BigDecimal, creditingPayable: Boolean) =
        LedgerEntryRequest.unlessZero(
            payableAccountCode,
            if (creditingPayable) EntryType.CREDIT else EntryType.DEBIT,
            taxAmount
        )

    private fun grossSalesLeg(taxAmount: BigDecimal, debitingGrossSales: Boolean) =
        LedgerEntryRequest.unlessZero(
            SystemAccount.GROSS_SALES.code,
            if (debitingGrossSales) EntryType.DEBIT else EntryType.CREDIT,
            taxAmount
        )

    private fun taxExpenseLeg(taxAmount: BigDecimal, debitingExpense: Boolean) =
        LedgerEntryRequest.unlessZero(
            SystemAccount.TAX_EXPENSE.code,
            if (debitingExpense) EntryType.DEBIT else EntryType.CREDIT,
            taxAmount
        )

    private fun payableAccountCodesByTaxTypeId(): Map<UUID, String> =
        orgJurisdictionTaxTypeFetcher.getAllDtos().associate { it.jurisdictionTaxTypeId to it.payableAccountCode }

    private fun throwMissingPayableAccountCodeException(taxEntryDto: TaxEntryDto): Nothing {
        throw RtsGenericException(
            "Missing payable account code for tax type ${taxEntryDto.taxTypeName} in jurisdiction ${taxEntryDto.jurisdictionName}. " +
                    "Please configure the payable account code for this tax type in the organization settings."
        )
    }
}
