package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.StockHeldBySales
import app.myliberty.easy_records.locations.business.stock.api.availability.StockAvailabilityFetcher
import app.myliberty.easy_records.locations.business.stock_reservation.api.ProductReservations
import app.myliberty.easy_records.locations.business.stock_reservation.api.StockReserver
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleSessionStockOverlay(
    private val stockAvailabilityFetcher: StockAvailabilityFetcher,
    private val stockReserver: StockReserver,
) {

    fun populate(saleSession: SaleSession): SaleSession {
        if (saleSession.originalStatus != SaleStatus.DRAFT) return saleSession
        if (saleSession.saleLines.isEmpty()) return saleSession
        val locationProductIds = saleSession.saleLines.map { it.locationProductId }.distinct()
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(locationProductIds)
        val reservationsByLocationProductId = reservationsOfTheSessionsOwnSale(saleSession, locationProductIds)
        val populatedSaleLines = saleSession.saleLines.map { saleSessionLine ->
            val availability = availabilityByLocationProductId.getValue(saleSessionLine.locationProductId)
            val stockHeld = stockHeldBySalesFor(saleSession, saleSessionLine, reservationsByLocationProductId)
            saleSessionLine.copy(
                storageQuantityOnHand = availability.quantityOnHand.add(stockHeld.storageQuantityTaken),
                storageQuantityReserved = availability.quantityReserved.subtract(stockHeld.storageQuantityReserved),
                storageQuantityAvailable = availability.quantityAvailable
                    .add(stockHeld.storageQuantityReserved)
                    .add(stockHeld.storageQuantityTaken),
            )
        }
        return saleSession.copy(saleLines = populatedSaleLines)
    }

    private fun reservationsOfTheSessionsOwnSale(
        saleSession: SaleSession,
        locationProductIds: List<UUID>,
    ): Map<UUID, ProductReservations> =
        if (saleSession.saleId == null) emptyMap() else stockReserver.loadReservationBreakdown(locationProductIds)

    // Shows the stock as if the sales this session replaces were already gone, which is how the save checks it.
    private fun stockHeldBySalesFor(
        saleSession: SaleSession,
        saleSessionLine: SaleSessionLine,
        reservationsByLocationProductId: Map<UUID, ProductReservations>,
    ): StockHeldBySales {
        saleSession.pendingConsolidation?.let { pendingConsolidation ->
            return pendingConsolidation.stockHeldBySourceSalesByLocationProductId[saleSessionLine.locationProductId]
                ?: StockHeldBySales.NONE
        }
        val ownReservation = saleSession.saleId?.let { saleId ->
            reservationsByLocationProductId[saleSessionLine.locationProductId]?.forSale(saleId)
        } ?: BigDecimal.ZERO
        return StockHeldBySales(storageQuantityReserved = ownReservation, storageQuantityTaken = BigDecimal.ZERO)
    }
}
