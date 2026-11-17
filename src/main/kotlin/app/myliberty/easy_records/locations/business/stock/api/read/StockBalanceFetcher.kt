package app.myliberty.easy_records.locations.business.stock.api.read

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.stock.api.fifo.StockEntryFifoDto
import app.myliberty.easy_records.locations.business.stock.stockEntryFifoComparator
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockBalanceFetcher(private val stockEntryRepository: StockEntryRepository) {

    fun getFifoEntriesByProduct(locationProductIds: List<UUID>): Map<UUID, List<StockEntryFifoDto>> =
        stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, entries) ->
                entries.sortedWith(stockEntryFifoComparator).map { entry -> entry.toFifoDto() }
            }

    fun getLatestBalances(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal> {
        val balancesByProduct = stockEntryRepository.sumRemainingByProducts(locationProductIds)
            .associateBy { it.getLocationProductId() }
        return locationProductIds.associateWith {
            balancesByProduct[it]?.getStorageRemainingQuantity() ?: BigDecimal.ZERO
        }
    }
}
