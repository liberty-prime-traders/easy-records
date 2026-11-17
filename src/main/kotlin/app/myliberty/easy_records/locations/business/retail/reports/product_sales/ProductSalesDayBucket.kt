package app.myliberty.easy_records.locations.business.retail.reports.product_sales

import java.time.OffsetDateTime
import java.util.UUID

data class ProductSalesDayBucket(
  val builtAt: OffsetDateTime,
  val rawRowsByLocationProductId: Map<UUID, ProductSalesSummaryRawRow>
)
