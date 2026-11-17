package app.myliberty.easy_records.locations.business.retail.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferReceiptLineRepository : JpaRepository<StockTransferReceiptLineEntity, UUID> {

    fun findByStockTransferReceiptId(stockTransferReceiptId: UUID): List<StockTransferReceiptLineEntity>

    fun findByStockTransferDispatchLineRef(stockTransferDispatchLineRef: String): StockTransferReceiptLineEntity?

    fun existsByStockTransferDispatchLineRef(stockTransferDispatchLineRef: String): Boolean
}
