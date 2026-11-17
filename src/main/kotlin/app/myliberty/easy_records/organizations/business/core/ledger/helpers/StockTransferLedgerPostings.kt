package app.myliberty.easy_records.organizations.business.core.ledger.helpers

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component

@Component
class StockTransferLedgerPostings(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository
) {

    // Fails instead of skipping: a skipped event writes no log row, so neither the sweeper nor a retry would revisit it
    // once the dispatch posts, and the in-transit balance would never clear.
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requireDispatchPosted(orderReferenceNumber: String, dispatchReferenceNumber: String) {
        val sourceLocationId = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber).sourceLocationId
        val dispatchPosted = ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
            dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_DISPATCH, sourceLocationId
        )
        if (!dispatchPosted) {
            throw LibertyGenericException(
                "Cannot clear in transit for $dispatchReferenceNumber: its " +
                    "${LedgerSourceType.STOCK_TRANSFER_DISPATCH.name} posting does not exist"
            )
        }
    }

    fun moveIntoTransit(amount: Money): List<LedgerEntryRequest> =
        LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY_IN_TRANSIT.code, amount.toBigDecimal()) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY.code, amount.toBigDecimal())

    fun moveOutOfTransit(amount: Money): List<LedgerEntryRequest> =
        LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, amount.toBigDecimal()) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY_IN_TRANSIT.code, amount.toBigDecimal())
}
