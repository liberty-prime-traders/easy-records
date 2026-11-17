package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxDirection
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntryCreateDto
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntryService
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxSourceType
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxAmountCalculator
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateService
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@TransactionalOnLocationSchema
class SaleTaxFinalizer(
    private val saleRepository: SaleRepository,
    private val taxEntryService: TaxEntryService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val taxRateService: TaxRateService,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun finalizeTaxForConfirm(saleEntity: SaleEntity) {
        val calculationDate = DateTimes.Local.atOrganizationZone(requireNotNull(saleEntity.dateSold))
        val fiscalPeriodId = fiscalPeriodService.requireOpenForDate(calculationDate)

        val activeTaxTypes = transactionLevelActiveTaxTypesForCalculation(calculationDate)
        val result = TaxAmountCalculator.calculate(saleEntity.taxableAmount(), activeTaxTypes)

        val namesByJurisdictionTaxTypeId = jurisdictionTaxTypeFetcher.getNames(result.entries.map { it.jurisdictionTaxTypeId })
        val taxEntries = result.entries.map { entry ->
            val names = namesByJurisdictionTaxTypeId.getValue(entry.jurisdictionTaxTypeId)
            TaxEntryCreateDto(
                sourceReferenceNumber = saleEntity.requiredReference(),
                sourceType = TaxSourceType.SALE,
                direction = TaxDirection.OUTPUT,
                taxTypeId = entry.jurisdictionTaxTypeId,
                taxTypeName = names.taxTypeName,
                jurisdictionName = names.jurisdictionName,
                fiscalPeriodId = fiscalPeriodId,
                calculationMethod = entry.calculationMethod,
                rate = entry.rate,
                taxIsBilledToCustomerSeparately = entry.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = entry.taxIsIncludedInTaxableAmount,
                taxableAmount = entry.taxableAmount,
                resolvedTaxableBase = entry.resolvedTaxableBase,
                taxAmount = entry.taxAmount
            )
        }
        taxEntryService.createAll(taxEntries)

        saleEntity.taxTotal = result.entries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.taxAmount }
        saleEntity.taxBilled = result.taxBilled
        saleRepository.save(saleEntity)
    }

    fun finalizeTaxForVoids(voidedSales: List<VoidedSale>) {
        val voidDateByReference = voidedSales.associate { it.sale.requiredReference() to it.voidDate }
        val originals = taxEntryService.findBySourceReferences(voidDateByReference.keys, TaxSourceType.SALE)
        if (originals.isEmpty()) return
        val fiscalPeriodIdByVoidDate = originals
            .map { voidDateByReference.getValue(it.sourceReferenceNumber) }
            .distinct()
            .associateWith { fiscalPeriodService.requireOpenForDate(it) }
        val reversals = originals.map { original ->
            TaxEntryCreateDto(
                sourceReferenceNumber = original.sourceReferenceNumber,
                sourceType = TaxSourceType.SALE_VOID,
                direction = original.direction,
                taxTypeId = original.taxTypeId,
                taxTypeName = original.taxTypeName,
                jurisdictionName = original.jurisdictionName,
                fiscalPeriodId = fiscalPeriodIdByVoidDate
                    .getValue(voidDateByReference.getValue(original.sourceReferenceNumber)),
                calculationMethod = original.calculationMethod,
                rate = original.rate,
                taxIsBilledToCustomerSeparately = original.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = original.taxIsIncludedInTaxableAmount,
                taxableAmount = original.taxableAmount.negate(),
                resolvedTaxableBase = original.resolvedTaxableBase.negate(),
                taxAmount = original.taxAmount.negate()
            )
        }
        taxEntryService.createAll(reversals)
    }

    private fun transactionLevelActiveTaxTypesForCalculation(calculationDate: LocalDate): List<TaxAmountCalculator.ActiveTaxTypeForCalculation> {
        val activeOrgTaxTypes = orgJurisdictionTaxTypeFetcher.getAllDtos()
            .filter { it.status == OrgJurisdictionTaxTypeStatus.ACTIVE }
        val rates = taxRateService.findActiveRateForDate(calculationDate)

        return activeOrgTaxTypes.mapNotNull { orgTaxType ->
            val taxType = jurisdictionTaxTypeFetcher.getTaxType(orgTaxType.jurisdictionTaxTypeId)
            if (taxType.taxApplicationLevel != TaxApplicationLevel.TRANSACTION) return@mapNotNull null
            if (TaxTrigger.SALE !in taxType.taxTriggers) return@mapNotNull null
            val rate = rates[orgTaxType.id] ?: return@mapNotNull null
            TaxAmountCalculator.ActiveTaxTypeForCalculation(
                orgJurisdictionTaxTypeId = orgTaxType.id,
                jurisdictionTaxTypeId = orgTaxType.jurisdictionTaxTypeId,
                calculationMethod = taxType.calculationMethod,
                activeTaxRateDto = rate
            )
        }
    }
}
