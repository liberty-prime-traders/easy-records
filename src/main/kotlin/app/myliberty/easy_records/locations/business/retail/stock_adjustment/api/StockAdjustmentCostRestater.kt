package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCostEntity
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCostRepository
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentRepository
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation

// Appends cost rows; the adjustment's own quantity, category and original cost are never edited.
@Service
@TransactionalOnLocationSchema
class StockAdjustmentCostRestater(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockAdjustmentCostRepository: StockAdjustmentCostRepository,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun restate(stockAdjustmentCostChanges: List<StockAdjustmentCostChange>) {
        if (stockAdjustmentCostChanges.isEmpty()) return
        val stockAdjustmentByReferenceNumber = stockAdjustmentRepository
            .findByReferenceNumberIn(stockAdjustmentCostChanges.map { it.adjustmentReferenceNumber })
            .associateBy { it.requiredReference() }
        val activeCostByStockAdjustmentId = stockAdjustmentCostRepository
            .findByStockAdjustmentIdInAndStatus(
                stockAdjustmentByReferenceNumber.values.map { it.id!! }, StockAdjustmentCostStatus.ACTIVE
            )
            .associateBy { it.stockAdjustmentId }

        stockAdjustmentCostChanges.forEach { stockAdjustmentCostChange ->
            val referenceNumber = stockAdjustmentCostChange.adjustmentReferenceNumber
            val stockAdjustment = stockAdjustmentByReferenceNumber[referenceNumber]
                ?: throw RtsGenericException("Stock adjustment $referenceNumber not found")
            val activeCost = activeCostByStockAdjustmentId[stockAdjustment.id!!]
                ?: throw RtsGenericException("Stock adjustment $referenceNumber has no cost record")
            val restatedTotalCost = activeCost.totalCost - stockAdjustmentCostChange.oldCost + stockAdjustmentCostChange.newCost
            if (restatedTotalCost.signum() < 0) {
                throw RtsGenericException("Stock adjustment $referenceNumber would have a negative cost after the revision")
            }
            // Flushed first: the unique index on ACTIVE rows is not deferrable and inserts flush before updates.
            activeCost.status = StockAdjustmentCostStatus.REPLACED
            stockAdjustmentCostRepository.saveAndFlush(activeCost)
            stockAdjustmentCostRepository.save(
                StockAdjustmentCostEntity(stockAdjustmentId = stockAdjustment.id!!, totalCost = restatedTotalCost)
            )
        }
    }
}
