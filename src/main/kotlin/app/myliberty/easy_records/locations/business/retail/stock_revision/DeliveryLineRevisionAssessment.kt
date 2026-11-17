package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.locations.business.retail.delivery.api.ValidatedDeliveryLineRevisionDto
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.util.UUID

data class DeliveryLineRevisionAssessment(
    val validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
    val quantityBreakdown: QuantityBreakdown,
    val replayResult: ReplayResult,
    val reservationShortfall: ReservationShortfall?,
    val replacementStockEntryId: UUID? = null,
) {
    val reservationProblems: List<BlockingProblem> = reservationShortfall?.blockingProblems ?: emptyList()
    val blockingProblems: List<BlockingProblem> = buildList {
        addAll(validatedDeliveryLineRevision.problems)
        addAll(replayResult.problems)
        addAll(reservationProblems)
    }
}
