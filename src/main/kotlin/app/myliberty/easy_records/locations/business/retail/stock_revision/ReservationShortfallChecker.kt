package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ReservationShortfallChecker(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
    private val saleDataFetcher: SaleDataFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    fun shortfallAfterPendingChange(
        locationProductId: UUID,
        pendingStorageQuantityChange: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): ReservationShortfall? {
        val productReservations = stockReserver.loadReservationBreakdown(listOf(locationProductId))[locationProductId]
            ?: return null
        val onHandAfterChange = ExactQuantity.ofStorage(
            stockBalanceFetcher.getLatestBalances(listOf(locationProductId)).getValue(locationProductId)
        ) + pendingStorageQuantityChange
        val reservedTotal = ExactQuantity.ofStorage(productReservations.total)
        if (reservedTotal <= onHandAfterChange) return null

        val productName = locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))
            .getValue(locationProductId).productName
        val referenceNumberBySaleId = saleDataFetcher.getReferenceNumbersById(productReservations.bySale.keys)
        val eventualOnHandBreakdown = quantityBreakdown.render(onHandAfterChange)
        val reservedBreakdown = quantityBreakdown.render(reservedTotal)
        val holdersText = if (productReservations.bySale.size == 1) {
            "the draft sale holds $reservedBreakdown"
        } else {
            "the draft sales hold $reservedBreakdown between them"
        }
        val effect = "After your change only $eventualOnHandBreakdown would be left in stock, but $holdersText."
        val blockingProblems = productReservations.bySale
            .map { (saleId, reservedQuantity) ->
                BlockingProblem(
                    documentReference = "${referenceNumberBySaleId.getValue(saleId)} (draft)",
                    description = "Draft sale holding " +
                        "${quantityBreakdown.render(ExactQuantity.ofStorage(reservedQuantity))} of $productName.",
                    effect = effect
                )
            }
            .sortedBy { it.documentReference }
        return ReservationShortfall(blockingProblems, productName, onHandAfterChange, reservedTotal)
    }
}
