package app.myliberty.easy_records.locations.business.retail.stock.api.revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryEntity
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.StockInboundLayers
import app.myliberty.easy_records.locations.business.retail.stock.StockReplacementLayerSpecs
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.FifoLayer
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.StockEntryFifoDto
import app.myliberty.easy_records.locations.business.retail.stock.stockEntryFifoComparator
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockReplacementLayerPreviewer(
    private val stockEntryRepository: StockEntryRepository,
) {

    fun previewReplacement(
        stockReplacementLayerRequest: StockReplacementLayerRequest
    ): ReplacementLayerPreview {
        val revisedStockEntry = findRevisedStockEntry(stockReplacementLayerRequest.revisedStockEntryId)
        val replacementEntry = StockInboundLayers.newLayer(
            StockReplacementLayerSpecs.of(stockReplacementLayerRequest, revisedStockEntry)
        )
        val otherEntriesInDrawOrder = stockEntryRepository
            .findFifoEntriesForProducts(listOf(stockReplacementLayerRequest.locationProductId))
            .filter { it.id != revisedStockEntry.id }
            .sortedWith(stockEntryFifoComparator)
        val (entriesDrawnBefore, entriesDrawnAfter) = otherEntriesInDrawOrder.partition {
            stockEntryFifoComparator.compare(it, replacementEntry) <= 0
        }

        val unsavedReplacement = unsavedFifoDtoOf(replacementEntry, stockReplacementLayerRequest.detailReference)
        val fifoEntries = buildList {
            addAll(entriesDrawnBefore.map { it.toFifoDto() })
            add(unsavedReplacement)
            addAll(entriesDrawnAfter.map { it.toFifoDto() })
        }
        return ReplacementLayerPreview(fifoEntries, unsavedReplacement.fifoLayer.stockEntryId)
    }

    private fun findRevisedStockEntry(revisedStockEntryId: UUID): StockEntryEntity =
        stockEntryRepository.findById(revisedStockEntryId).orElseThrow {
            RtsGenericException("The stock of this delivery could not be found. Please contact support.")
        }

    private fun unsavedFifoDtoOf(unsavedEntry: StockEntryEntity, referenceNumber: String): StockEntryFifoDto {
        val unsavedFifoLayer = FifoLayer(
            stockEntryId = UUID.randomUUID(),
            storageQuantityRemaining = unsavedEntry.storageQuantityRemaining,
            costRemaining = unsavedEntry.costRemaining,
            storageBatchSize = unsavedEntry.storageBatchSize,
            batchCost = unsavedEntry.batchCost,
        )
        return StockEntryFifoDto(
            referenceNumber = referenceNumber,
            locationProductId = unsavedEntry.locationProductId,
            unitCost = unsavedEntry.unitCost,
            fifoLayer = unsavedFifoLayer,
        )
    }
}
