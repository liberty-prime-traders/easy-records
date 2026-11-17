package app.myliberty.easy_records.locations.business.retail.purchase.api.search

import app.myliberty.easy_records.util.enums.PaymentStatus
import java.math.BigDecimal
import java.util.UUID

data class PurchasePaymentStatusSummaryDto(
  val paymentStatus: PaymentStatus,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalDelivered: BigDecimal,
  val totalPaid: BigDecimal
) {
  val totalOutstanding: BigDecimal = totalDelivered - totalPaid
}

data class PurchaseSupplierSummaryDto(
  val supplierId: UUID,
  val supplierName: String,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalDelivered: BigDecimal,
  val totalPaid: BigDecimal
) {
  val totalOutstanding: BigDecimal = totalDelivered - totalPaid
}

data class PurchaseSearchSummaryDto(
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalDelivered: BigDecimal,
  val totalPaid: BigDecimal,
  val byPaymentStatus: List<PurchasePaymentStatusSummaryDto>,
  val bySupplier: List<PurchaseSupplierSummaryDto>?
) {
  val totalOutstanding: BigDecimal = totalDelivered - totalPaid
}
