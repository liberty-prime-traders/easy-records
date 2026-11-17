package app.myliberty.easy_records.locations.business.stock_adjustment_backfill

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock_adjustment.api.StockAdjustmentBackfillStore
import app.myliberty.easy_records.util.business.reference_number.ReferenceNumberGenerator
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class LocationStockAdjustmentBackfiller(
    private val stockAdjustmentBackfillStore: StockAdjustmentBackfillStore,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
) {

    fun insertNextBatchOfCostRecords(): Int {
        val candidates = stockAdjustmentBackfillStore.findAdjustmentsMissingCostRecord(BATCH_SIZE)
        if (candidates.isEmpty()) return 0
        val referenceNumbers = referenceNumberGenerator.generateBulk(TableName.STOCK_ADJUSTMENT_COST, candidates.size)
        stockAdjustmentBackfillStore.insertCostRecords(candidates, referenceNumbers)
        return candidates.size
    }

    fun countsAfterInserting(insertedCostRecordCount: Long) = StockAdjustmentBackfillCounts(
        insertedCostRecordCount = insertedCostRecordCount,
        adjustmentCount = stockAdjustmentBackfillStore.countAdjustments(),
        activeCostRecordCount = stockAdjustmentBackfillStore.countActiveCostRecords(),
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}
