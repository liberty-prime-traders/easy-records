package app.myliberty.easy_records.locations.business.location_product

import java.util.UUID

interface LocationProductSummaryView {
    val id: UUID
    val referenceNumber: String
    val orgProductId: UUID
    val productName: String
    val productGroupName: String
    val baseUnitId: UUID
}
