package app.myliberty.easy_records.cross_tier.expense.operation

import app.myliberty.easy_records.cross_tier.expense.ExpenseTier
import app.myliberty.easy_records.cross_tier.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
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
            ?: throw RtsGenericException("Expense void $expenseVoidId not found")
        expenseEvents.publishVoided(expenseVoidDto, expenseLookup.requireExpenseById(expenseVoidDto.expenseId))
    }

    fun reissuePaymentRecorded(paymentId: UUID) {
        val expensePaymentDto = requirePaymentById(paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentRecorded(expensePaymentDto, expenseDto)
    }

    fun reissuePaymentVoided(paymentVoidId: UUID) {
        val expensePaymentVoidDto = expenseStore.findPaymentVoidById(paymentVoidId)
            ?: throw RtsGenericException("Expense payment void $paymentVoidId not found")
        val expensePaymentDto = requirePaymentById(expensePaymentVoidDto.paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentVoided(expensePaymentVoidDto, expensePaymentDto, expenseDto)
    }

    private fun requirePaymentById(paymentId: UUID): ExpensePaymentDto =
        expenseStore.findPaymentById(paymentId) ?: throw RtsGenericException("Expense payment $paymentId not found")
}
