package app.myliberty.easy_records.locations.business.product_purchases_report

import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesDeliveredRange(val fromInclusive: OffsetDateTime, val beforeExclusive: OffsetDateTime)

data class ProductPurchasesSummaryRawRow(
  val locationProductId: UUID,
  val categoryId: UUID,
  val deliveryLineCount: Long,
  val storageQuantity: BigDecimal,
  val totalCost: BigDecimal
)

data class ProductPurchasesDeliveryLineRawRow(
  val id: UUID,
  val purchaseReferenceNumber: String,
  val deliveryReferenceNumber: String,
  val deliveredAt: OffsetDateTime,
  val supplierId: UUID,
  val unitId: UUID,
  val userProvidedQuantityDelivered: BigDecimal,
  val storageUnitsPerUserProvidedUnit: Long,
  val unitCost: BigDecimal,
  val status: PurchaseDeliveryLineStatus
)
