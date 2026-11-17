package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal

data class StockHeldBySales(
    val storageQuantityReserved: BigDecimal,
    val storageQuantityTaken: BigDecimal,
) {

    operator fun plus(other: StockHeldBySales): StockHeldBySales = StockHeldBySales(
        storageQuantityReserved = storageQuantityReserved + other.storageQuantityReserved,
        storageQuantityTaken = storageQuantityTaken + other.storageQuantityTaken,
    )

    companion object {
        val NONE = StockHeldBySales(BigDecimal.ZERO, BigDecimal.ZERO)
    }
}
