package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferDispatchDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferOrderRef: String,
    val status: StockTransferStatus,
    val dispatchedById: UUID?,
    val dispatchedAt: OffsetDateTime?,
    val notes: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
