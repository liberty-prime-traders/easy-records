package app.myliberty.easy_records.locations.business.stock.api.revision

import app.myliberty.easy_records.locations.business.stock.api.fifo.StockEntryFifoDto
import java.util.UUID

data class ReplacementLayerPreview(
    val fifoEntries: List<StockEntryFifoDto>,
    val replacementStockEntryId: UUID
)
