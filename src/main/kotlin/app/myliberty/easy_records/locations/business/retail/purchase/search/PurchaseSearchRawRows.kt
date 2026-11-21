package app.myliberty.easy_records.locations.business.retail.purchase.search

import java.math.BigDecimal
import java.util.UUID

data class PurchasePaymentStatusSummaryRawRow(
  val paymentStatusCode: String,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalDelivered: BigDecimal,
  val totalPaid: BigDecimal
)

data class PurchaseSupplierSummaryRawRow(
  val supplierId: UUID,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalDelivered: BigDecimal,
  val totalPaid: BigDecimal
)
