package app.myliberty.easy_records.locations.business.sale.search

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSearchRawRow(
  val id: UUID,
  val referenceNumber: String,
  val contactId: UUID,
  val soldByUserId: UUID?,
  val dateSold: OffsetDateTime?,
  val createdOn: OffsetDateTime,
  val status: SaleStatus,
  val paymentStatus: PaymentStatus,
  val receivableTotal: BigDecimal
)

data class SaleStatusSummaryRawRow(
  val status: SaleStatus,
  val saleCount: Long,
  val receivableTotal: BigDecimal,
  val discountTotal: BigDecimal,
  val paidTotal: BigDecimal,
  val outstandingTotal: BigDecimal,
  val creditTotal: BigDecimal
)
