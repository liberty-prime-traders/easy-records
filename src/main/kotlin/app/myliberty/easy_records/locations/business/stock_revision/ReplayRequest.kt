package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.locations.business.stock.api.revision.ConsumerReplayPlan
import app.myliberty.easy_records.locations.business.stock.api.fifo.StockEntryFifoDto
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown

data class ReplayRequest(
    val stockRevisionReferenceNumber: String,
    val consumptionsOnReplacedLayer: List<StockEntryConsumption>,
    val productBeingReplayed: ProductBeingReplayed
)

data class ProductBeingReplayed(
    val productName: String,
    val quantityBreakdown: QuantityBreakdown,
    val fifoEntriesAvailableForConsumption: List<StockEntryFifoDto>,
)

data class ReplayResult(
    val consumerReplayPlans: List<ConsumerReplayPlan>,
    val problems: List<BlockingProblem>,
    val totalTakenByConsumers: ExactQuantity = ExactQuantity.ZERO,
    val movementTypesOfUncoveredConsumers: Set<MovementType> = emptySet(),
)
