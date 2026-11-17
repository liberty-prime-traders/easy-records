package app.myliberty.easy_records.locations.business.retail.stock_transfer

import java.time.OffsetDateTime
import java.util.UUID

data class ReceiptSnapshot(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferReceiptStatus,
    val receivedById: UUID,
    val receivedAt: OffsetDateTime,
    val notes: String?,
    val confirmedDispatchLineReferenceNumbers: Set<String>
)
