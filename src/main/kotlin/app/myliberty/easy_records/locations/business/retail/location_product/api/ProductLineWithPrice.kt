package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

// unitPrice is derived for display and never stored; discounts and adjustments work from the gross lineTotal
interface ProductLineWithPrice {
    val lineKey: UUID
    val locationProductId: UUID
    val userProvidedQuantity: BigDecimal
    val lineTotal: BigDecimal

    val unitPrice: BigDecimal
        get() = if (userProvidedQuantity.signum() == 0) BigDecimal.ZERO else Decimals.Scale4.divide(lineTotal, userProvidedQuantity)
}
