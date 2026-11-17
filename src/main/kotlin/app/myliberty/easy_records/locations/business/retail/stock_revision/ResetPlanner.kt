package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.stock.api.read.StockEntryFetcher
import app.myliberty.easy_records.locations.business.stock.api.revision.StockEntryResetPlan
import app.myliberty.easy_records.locations.business.stock.api.StockEntryStatus
import app.myliberty.easy_records.util.exceptions.BlockedChangeException
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ResetPlanner(
    private val stockEntryFetcher: StockEntryFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher
) {

    fun plan(
        stockEntryId: UUID,
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): StockEntryResetPlan {
        val stockEntryDto = stockEntryFetcher.get(stockEntryId)
        if (stockEntryDto.status == StockEntryStatus.REPLACED) {
            throw BlockedChangeException.contactSupport(
                listOf(
                    BlockingProblem(
                        documentReference = stockEntryDto.referenceNumber,
                        description = "This stock record was already replaced by an earlier change.",
                        effect = "Change the latest delivery line instead."
                    )
                )
            )
        }
        val locationProductId = stockEntryDto.locationProductId
        val productBalance = stockBalanceFetcher.getLatestBalances(
            listOf(locationProductId)
        ).getValue(locationProductId)

        val restoration = StockEntryRestorer.restore(
            stockEntryConsumptions, stockEntryDto, stockRevisionReferenceNumber,
            ExactQuantity.ofStorage(productBalance)
        )
        val drainDraft = StockEntryDrainer.drain(
            stockEntryDto = stockEntryDto,
            restoredQuantity = restoration.restoredQuantity,
            restoredCost = restoration.restoredCost,
            stockRevisionReferenceNumber = stockRevisionReferenceNumber,
            balanceBeforeDrain = restoration.endingBalance,
            quantityBreakdown = quantityBreakdown
        )
        return StockEntryResetPlan(
            stockEntryId,
            locationProductId,
            restoration.reversalDrafts + listOfNotNull(drainDraft)
        )
    }
}
