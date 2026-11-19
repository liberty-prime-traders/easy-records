package app.myliberty.easy_records.locations.business.retail.sale_session.api

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
