package app.myliberty.easy_records.locations.business.retail.opening_stock.api

import java.math.BigDecimal
import java.util.UUID

data class OpeningStockAmountsDto(
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val unitCost: BigDecimal
)
