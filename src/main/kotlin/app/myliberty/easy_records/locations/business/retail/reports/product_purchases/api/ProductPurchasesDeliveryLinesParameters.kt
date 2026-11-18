package app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesDeliveryLinesParameters(
  val deliveredFrom: OffsetDateTime?,
  val deliveredBefore: OffsetDateTime?,
  val supplierIds: List<UUID> = emptyList()
)
