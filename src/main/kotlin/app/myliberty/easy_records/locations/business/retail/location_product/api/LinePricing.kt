package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal

object LinePricing {

    fun overrideTotal(userProvidedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        Decimals.MoneyScale.multiply(userProvidedUnitPrice, userProvidedQuantity)

    // Positive when the override total is lower than the line total (a discount), negative for a surcharge.
    fun overrideDifference(lineTotal: BigDecimal, userProvidedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        lineTotal.subtract(overrideTotal(userProvidedUnitPrice, userProvidedQuantity))
}
