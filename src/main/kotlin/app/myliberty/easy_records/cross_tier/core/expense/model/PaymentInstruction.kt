package app.myliberty.easy_records.cross_tier.core.expense.model

import java.time.LocalDate
import java.util.UUID

data class PaymentInstruction(
    val paymentMethodId: UUID,
    val paymentReference: String? = null,
    val paymentDate: LocalDate? = null
)
