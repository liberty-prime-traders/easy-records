package app.myliberty.easy_records.locations.business.stock.api.availability

import app.myliberty.easy_records.locations.business.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.stock_reservation.api.StockReserver
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

data class StockAvailability(
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
) {
    val quantityAvailable: BigDecimal = quantityOnHand - quantityReserved
}

@Component
class StockAvailabilityFetcher(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
) {

    fun fetch(locationProductIds: Collection<UUID>): Map<UUID, StockAvailability> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reservationsByLocationProductId = stockReserver.loadReservationBreakdown(locationProductIds)
        return locationProductIds.associateWith { locationProductId ->
            val quantityOnHand = balancesByLocationProductId.getValue(locationProductId)
            val quantityReserved = reservationsByLocationProductId[locationProductId]?.total ?: BigDecimal.ZERO
            StockAvailability(
                quantityOnHand = quantityOnHand,
                quantityReserved = quantityReserved,
            )
        }
    }
}
