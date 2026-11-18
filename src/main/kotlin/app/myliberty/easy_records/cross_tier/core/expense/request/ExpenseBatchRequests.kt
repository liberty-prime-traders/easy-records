package app.myliberty.easy_records.cross_tier.core.expense.request

import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseRowCommand
import app.myliberty.easy_records.cross_tier.core.expense.model.PaymentInstruction
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.LocalDate
import java.util.UUID

data class StandaloneExpenseBatchRequest(
    val description: String,
    val expenseDate: LocalDate,
    val rows: List<StandaloneExpenseRowRequest>
)

data class StandaloneExpenseRowRequest(
    val payeeContactId: UUID,
    val expenseTypeId: UUID,
    val amount: PositiveMoney,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
) {
    fun toRowCommand() = ExpenseRowCommand(expenseTypeId, payeeContactId, amount.toBigDecimal(), description, expenseDateOverride, settlement)
}

data class WageExpenseBatchRequest(
    val expenseDate: LocalDate,
    val rows: List<WageExpenseRowRequest>
)

data class WageExpenseRowRequest(
    val employeeContactId: UUID,
    val amount: PositiveMoney,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
)

data class PurchaseExpenseBatchRequest(
    val purchaseReference: String,
    val expenseDate: LocalDate,
    val rows: List<PurchaseExpenseRowRequest>
)

data class PurchaseExpenseRowRequest(
    val payeeContactId: UUID? = null,
    val expenseTypeId: UUID,
    val amount: PositiveMoney,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
)

data class SaleExpenseBatchRequest(
    val saleReference: String,
    val expenseDate: LocalDate,
    val rows: List<RequiredPayeeExpenseRowRequest>
)

data class StockTransferExpenseBatchRequest(
    val transferReference: String,
    val expenseDate: LocalDate,
    val rows: List<RequiredPayeeExpenseRowRequest>
)

data class RequiredPayeeExpenseRowRequest(
    val payeeContactId: UUID,
    val expenseTypeId: UUID,
    val amount: PositiveMoney,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
) {
    fun toRowCommand() = ExpenseRowCommand(expenseTypeId, payeeContactId, amount.toBigDecimal(), description, expenseDateOverride, settlement)
}
