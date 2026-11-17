package app.myliberty.easy_records.cross_tier.core.expense.request

import app.myliberty.easy_records.cross_tier.core.expense.model.PaymentInstruction
import app.myliberty.easy_records.util.business.PositiveMoney

data class ExpensePaymentCreateRequest(
    val expenseReference: String,
    val settlement: PaymentInstruction,
    val amount: PositiveMoney
)

data class ExpenseVoidRequest(
    val expenseReference: String,
    val reason: String
)

data class ExpensePaymentVoidRequest(
    val paymentReference: String,
    val reason: String
)
