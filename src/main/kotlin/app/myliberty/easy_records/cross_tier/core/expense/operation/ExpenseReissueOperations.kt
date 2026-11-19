package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseTier
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

class ExpenseReissueOperations(
    expenseTier: ExpenseTier,
    private val expenseEvents: ExpenseEvents,
    private val expenseLookup: ExpenseLookup
) {

    private val expenseStore = expenseTier.expenseStore

    fun reissueRecorded(expenseId: UUID) {
        expenseEvents.publishRecorded(expenseLookup.requireExpenseById(expenseId))
    }

    fun reissueVoided(expenseVoidId: UUID) {
        val expenseVoidDto = expenseStore.findExpenseVoidById(expenseVoidId)
            ?: throw LibertyGenericException("Expense void $expenseVoidId not found")
        expenseEvents.publishVoided(expenseVoidDto, expenseLookup.requireExpenseById(expenseVoidDto.expenseId))
    }

    fun reissuePaymentRecorded(paymentId: UUID) {
        val expensePaymentDto = requirePaymentById(paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentRecorded(expensePaymentDto, expenseDto)
    }

    fun reissuePaymentVoided(paymentVoidId: UUID) {
        val expensePaymentVoidDto = expenseStore.findPaymentVoidById(paymentVoidId)
            ?: throw LibertyGenericException("Expense payment void $paymentVoidId not found")
        val expensePaymentDto = requirePaymentById(expensePaymentVoidDto.paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentVoided(expensePaymentVoidDto, expensePaymentDto, expenseDto)
    }

    private fun requirePaymentById(paymentId: UUID): ExpensePaymentDto =
        expenseStore.findPaymentById(paymentId) ?: throw LibertyGenericException("Expense payment $paymentId not found")
}
