package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_revision.api.AdjustmentCostChange
import app.myliberty.easy_records.locations.business.retail.stock_revision.api.RevisionLedgerEntries
import app.myliberty.easy_records.organizations.business.ledger.api.PostedStockAdjustmentAccountsReader
import org.springframework.stereotype.Component

// For a preview, which has nothing to wait for: an adjustment whose original posting is not there to read (cost zero, or
// not posted yet) uses the accounts the category rules give.
@Component
@TransactionalOnLocationSchema(readOnly = true)
class AdjustmentCostChangeResolver(
    private val stockAdjustmentRestatements: StockAdjustmentRestatements,
    private val postedStockAdjustmentAccountsReader: PostedStockAdjustmentAccountsReader
) {

    fun byAdjustmentReference(consumerCostChanges: List<ConsumerCostChange>): Map<String, AdjustmentCostChange> {
        val restatements = stockAdjustmentRestatements.of(consumerCostChanges)
        if (restatements.isEmpty()) return emptyMap()
        val postedAccounts = postedStockAdjustmentAccountsReader
            .findByAdjustmentReferenceNumber(restatements.map { it.adjustmentReferenceNumber }.toSet())
        return restatements.associate {
            it.adjustmentReferenceNumber to
                RevisionLedgerEntries.adjustmentCostChangeOf(it, postedAccounts[it.adjustmentReferenceNumber])
        }
    }
}
