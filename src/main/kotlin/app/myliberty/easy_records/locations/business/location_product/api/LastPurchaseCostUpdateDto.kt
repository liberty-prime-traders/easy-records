package app.myliberty.easy_records.locations.business.location_product.api

import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class LastPurchaseCostUpdateDto(
    val locationProductId: UUID,
    val unitCost: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
) {

    fun costPerBaseUnit(storageUnitsPerBaseUnit: Long): BigDecimal =
        Decimals.Scale4.divide(
            unitCost.multiply(BigDecimal.valueOf(storageUnitsPerBaseUnit)),
            BigDecimal.valueOf(storageUnitsPerUserProvidedUnit),
        )
}
