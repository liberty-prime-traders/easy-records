package app.myliberty.easy_records.locations.business.retail.location_product

import java.math.BigDecimal
import java.util.UUID

interface LocationProductIdentityView {
    val id: UUID
    val orgProductId: UUID
    val baseUnitId: UUID
}

interface LocationProductDefaultSalePriceView {
    val id: UUID
    val defaultSalePrice: BigDecimal?
}

interface LocationProductReferenceView {
    val id: UUID
    val referenceNumber: String
}
