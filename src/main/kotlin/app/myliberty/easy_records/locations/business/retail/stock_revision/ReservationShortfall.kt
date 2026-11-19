package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity

data class ReservationShortfall(
    val blockingProblems: List<BlockingProblem>,
    val productName: String,
    val quantityLeftInStock: ExactQuantity,
    val quantityHeld: ExactQuantity,
)
