package app.myliberty.easy_records.locations.business.retail.stock_transfer.api

import app.myliberty.easy_records.locations.business.retail.stock_transfer.ReconciledTransferLine
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferPerspective
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptStatus
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferSummaryDto
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferResponse(
    val summary: StockTransferSummaryDto,
    val dispatch: StockTransferDispatchResponseDto,
    val receipt: StockTransferReceiptResponseDto?,
    val perspective: StockTransferPerspective
)

data class StockTransferDispatchResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferStatus,
    val dispatchedById: UUID?,
    val dispatchedAt: OffsetDateTime?,
    val notes: String?,
    val lines: List<ReconciledTransferLine>
)

data class StockTransferReceiptResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferReceiptStatus,
    val receivedById: UUID,
    val receivedAt: OffsetDateTime,
    val notes: String?,
    val lines: List<ReconciledTransferLine>
)

