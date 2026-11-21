package app.myliberty.easy_records.locations.business.retail.location_product.api

import java.util.UUID

data class LocationProductSummaryDto(
    val id: UUID,
    val referenceNumber: String,
    val orgProductId: UUID,
    val productName: String,
    val productGroupName: String,
    val baseUnitId: UUID,
) {
    val label: String = "$referenceNumber - $productGroupName - $productName"
    val printLabel: String = "$referenceNumber - $productName"
}
