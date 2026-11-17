package app.myliberty.easy_records.locations.business.retail.product_purchases_report.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesReportParameters(
  val deliveredFrom: OffsetDateTime?,
  val deliveredBefore: OffsetDateTime?,
  val locationProductIds: List<UUID> = emptyList(),
  val categoryIds: List<UUID> = emptyList(),
  val supplierIds: List<UUID> = emptyList()
)
