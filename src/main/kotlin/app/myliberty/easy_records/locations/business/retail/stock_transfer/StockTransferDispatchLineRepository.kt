package app.myliberty.easy_records.locations.business.retail.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferDispatchLineRepository : JpaRepository<StockTransferDispatchLineEntity, UUID> {

    fun findByStockTransferDispatchId(stockTransferDispatchId: UUID): List<StockTransferDispatchLineEntity>

    fun findByReferenceNumber(referenceNumber: String): StockTransferDispatchLineEntity?

}
