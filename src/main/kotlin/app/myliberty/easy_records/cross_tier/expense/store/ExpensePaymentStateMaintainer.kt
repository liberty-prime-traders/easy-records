package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
