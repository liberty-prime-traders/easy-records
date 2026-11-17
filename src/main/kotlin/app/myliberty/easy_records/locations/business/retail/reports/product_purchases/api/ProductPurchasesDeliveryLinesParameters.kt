package app.myliberty.easy_records.locations.business.retail.product_purchases_report.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesDeliveryLinesParameters(
  val deliveredFrom: OffsetDateTime?,
  val deliveredBefore: OffsetDateTime?,
  val supplierIds: List<UUID> = emptyList()
)
