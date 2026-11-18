package app.myliberty.easy_records.locations.business.retail.sale_session.api

import java.math.BigDecimal

data class LineQuantities(
    val userProvidedQuantity: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
    val storageUnitsPerBaseUnit: Long
)
