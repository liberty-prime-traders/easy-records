package app.myliberty.easy_records.locations.business.retail.last_purchase_cost_backfill

import java.util.UUID

data class LastPurchaseCostBatch(
    val updatedProductCount: Int,
    val lastLocationProductId: UUID,
)
