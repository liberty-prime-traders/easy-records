package app.myliberty.easy_records.locations.business.stock.api.availability

import app.myliberty.easy_records.locations.business.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.stock_reservation.api.StockReserver
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class StockAvailabilityValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun guardSufficientStock(
        storageQuantityNeededByLocationProductId: Map<UUID, ExactQuantity>,
        productLabelByLocationProductId: Map<UUID, String> = emptyMap(),
    ) {
        if (storageQuantityNeededByLocationProductId.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, storageQuantityNeededByLocationProductId.keys)
        val balances = stockBalanceFetcher.getLatestBalances(storageQuantityNeededByLocationProductId.keys)
        val reservations = stockReserver.loadReservationBreakdown(storageQuantityNeededByLocationProductId.keys)
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
            storageQuantityNeededByLocationProductId.keys
        )
        storageQuantityNeededByLocationProductId.forEach { (locationProductId, storageQuantityNeeded) ->
            val reservedTotal = reservations[locationProductId]?.total ?: BigDecimal.ZERO
            val available = ExactQuantity.ofStorage(balances.getValue(locationProductId)) - ExactQuantity.ofStorage(reservedTotal)
            if (available < storageQuantityNeeded) {
                val label = productLabelByLocationProductId[locationProductId] ?: locationProductId.toString()
                val breakdown = breakdownByLocationProductId.getValue(locationProductId)
                throw RtsGenericException(
                    "Insufficient stock for $label. Available: ${breakdown.render(available)}, Requested: ${breakdown.render(storageQuantityNeeded)}"
                )
            }
        }
    }
}
