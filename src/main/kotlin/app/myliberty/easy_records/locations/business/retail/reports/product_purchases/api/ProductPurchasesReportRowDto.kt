package app.myliberty.easy_records.locations.business.retail.product_purchases_report.api

import java.math.BigDecimal
import java.util.UUID

data class ProductPurchasesReportRowDto(
  val locationProductId: UUID,
  val referenceNumber: String,
  val productName: String,
  val productGroupName: String,
  val categoryName: String,
  val deliveryLineCount: Long,
  val receivedQuantityBreakdown: String,
  val totalCost: BigDecimal,
  val averageCostPerBaseUnit: String?
)
