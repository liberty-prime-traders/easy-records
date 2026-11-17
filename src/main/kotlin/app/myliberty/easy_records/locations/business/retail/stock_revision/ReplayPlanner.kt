package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.stock.api.revision.ConsumerReplayPlan
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.StockEntryFifoDto
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockEntryFetcher
import app.myliberty.easy_records.util.exceptions.BlockedChangeException
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ReplayPlanner(
    private val stockEntryFetcher: StockEntryFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun planReplayForExecution(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): List<ConsumerReplayPlan> {
        if (consumptionsOnReplacedLayer.isEmpty()) return emptyList()
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        val replayResult = planReplayForPreview(
            consumptionsOnReplacedLayer,
            stockEntryFetcher.findWithStockLeftInDrawOrder(locationProductId),
            stockRevisionReferenceNumber,
            quantityBreakdown
        )
        BlockedChangeException.throwIfAny(replayResult.problems)
        return replayResult.consumerReplayPlans
    }

    fun planReplayForPreview(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        fifoEntriesAvailableForConsumption: List<StockEntryFifoDto>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): ReplayResult {
        if (consumptionsOnReplacedLayer.isEmpty()) return ReplayResult(emptyList(), emptyList())
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        return ConsumerReplayer.replay(
            ReplayRequest(
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                consumptionsOnReplacedLayer = consumptionsOnReplacedLayer,
                productBeingReplayed = ProductBeingReplayed(
                    productName = locationProductDataFetcher.findSummaryByIds(
                        listOf(locationProductId)
                    ).getValue(locationProductId).productName,
                    quantityBreakdown = quantityBreakdown,
                    fifoEntriesAvailableForConsumption = fifoEntriesAvailableForConsumption,
                )
            )
        )
    }
}
