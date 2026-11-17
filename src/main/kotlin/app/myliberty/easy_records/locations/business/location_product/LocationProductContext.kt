package app.myliberty.easy_records.locations.business.location_product

import java.math.BigDecimal

data class LocationProductContext(
    val unitName: String?,
    val balance: BigDecimal? = null,
    val openingStockQuantity: BigDecimal? = null,
    val openingStockUnitCost: BigDecimal? = null,
    val balanceBreakdown: String? = null,
    val openingStockBreakdown: String? = null
)
