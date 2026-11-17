package app.myliberty.easy_records.locations.business.stock_transfer

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferReceiptLineDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferReceiptId: UUID,
    val stockTransferDispatchLineRef: String,
    val locationProductId: UUID,
    val storageQuantityReceived: BigDecimal,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
