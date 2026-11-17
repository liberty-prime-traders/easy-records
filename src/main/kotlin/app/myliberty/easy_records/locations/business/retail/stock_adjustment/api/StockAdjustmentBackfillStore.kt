package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCostEntity
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCostRepository
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockAdjustmentBackfillStore(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockAdjustmentCostRepository: StockAdjustmentCostRepository,
) {

    fun findAdjustmentsMissingCostRecord(batchSize: Int): List<StockAdjustmentCostBackfillCandidate> =
        stockAdjustmentRepository.findAdjustmentsMissingCostRecord(PageRequest.of(0, batchSize))

    fun insertCostRecords(candidates: List<StockAdjustmentCostBackfillCandidate>, referenceNumbers: List<String>) {
        stockAdjustmentCostRepository.saveAllAndFlush(candidates.mapIndexed { index, candidate ->
            StockAdjustmentCostEntity(stockAdjustmentId = candidate.stockAdjustmentId, totalCost = candidate.totalCost)
                .also {
                    it.referenceNumber = referenceNumbers[index]
                    it.createdOn = candidate.adjustedAt
                }
        })
    }

    fun countAdjustments(): Long = stockAdjustmentRepository.count()

    fun countActiveCostRecords(): Long = stockAdjustmentCostRepository.countByStatus(StockAdjustmentCostStatus.ACTIVE)
}
