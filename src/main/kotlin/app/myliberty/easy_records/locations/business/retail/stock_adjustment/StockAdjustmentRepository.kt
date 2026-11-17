package app.myliberty.easy_records.locations.business.retail.stock_adjustment

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCostBackfillCandidate
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockAdjustmentRepository : JpaRepository<StockAdjustmentEntity, UUID> {

    fun findByReferenceNumberIn(referenceNumbers: Collection<String>): List<StockAdjustmentEntity>

    @Query(
        """
        SELECT NEW app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCostBackfillCandidate(
            stockAdjustment.id, stockAdjustment.totalCost, stockAdjustment.createdOn)
        FROM StockAdjustmentEntity stockAdjustment
        WHERE NOT EXISTS (
            SELECT 1 FROM StockAdjustmentCostEntity stockAdjustmentCost
            WHERE stockAdjustmentCost.stockAdjustmentId = stockAdjustment.id)
        ORDER BY stockAdjustment.id
        """
    )
    fun findAdjustmentsMissingCostRecord(pageable: Pageable): List<StockAdjustmentCostBackfillCandidate>
}
