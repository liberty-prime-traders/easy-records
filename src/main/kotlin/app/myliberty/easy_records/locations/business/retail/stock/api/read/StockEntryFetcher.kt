package app.myliberty.easy_records.locations.business.retail.stock.api.read

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.StockEntryFifoDto
import app.myliberty.easy_records.locations.business.retail.stock.stockEntryFifoComparator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockEntryFetcher(
    private val stockEntryRepository: StockEntryRepository,
) {

    fun get(stockEntryId: UUID): StockEntryDto =
        StockEntryDto.of(
            stockEntryRepository.findById(stockEntryId).orElseThrow { LibertyGenericException(
                "Stock entry $stockEntryId not found."
            ) }
        )

    fun findWithStockLeftInDrawOrder(locationProductId: UUID): List<StockEntryFifoDto> =
        stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId))
            .sortedWith(stockEntryFifoComparator)
            .map { it.toFifoDto() }
}
