package app.myliberty.easy_records.cross_tier.core.expense.store

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentStateBase
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpensePaymentStateRepositoryBase
import app.myliberty.easy_records.util.business.PaymentStatusResolver
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

object ExpensePaymentStateMaintainer {

    fun <STATE : ExpensePaymentStateBase> refresh(
        expensePaymentStateRepository: ExpensePaymentStateRepositoryBase<STATE>,
        expenseDtos: Collection<ExpenseDto>,
        paidTotalsByExpenseId: Map<UUID, BigDecimal>
    ) {
        if (expenseDtos.isEmpty()) return
        val expenseAmountsById = expenseDtos.associate { it.id to it.amount }
        // The states load first so a writer that skipped the expense lock fails on @Version instead of persisting a stale total.
        val expensePaymentStates = expensePaymentStateRepository.findByExpenseIdIn(expenseAmountsById.keys)
        val expenseIdsMissingState = expenseAmountsById.keys - expensePaymentStates.map { it.expenseId }.toSet()
        if (expenseIdsMissingState.isNotEmpty()) {
            throw RtsGenericException("Expense payment state is missing for expenses $expenseIdsMissingState")
        }
        expensePaymentStates.forEach { expensePaymentState ->
            val amountPaid = paidTotalsByExpenseId.getValue(expensePaymentState.expenseId)
            expensePaymentState.amountPaid = amountPaid
            expensePaymentState.paymentStatus =
                PaymentStatusResolver.resolve(amountPaid, expenseAmountsById.getValue(expensePaymentState.expenseId))
        }
        expensePaymentStateRepository.saveAll(expensePaymentStates)
    }
}
