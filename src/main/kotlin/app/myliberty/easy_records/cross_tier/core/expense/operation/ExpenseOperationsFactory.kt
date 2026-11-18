package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseTier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ExpenseOperationsFactory(
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun operationsFor(expenseTier: ExpenseTier): TierExpenseOperations {
        val expenseEvents = ExpenseEvents(expenseTier, applicationEventPublisher)
        val expenseLookup = ExpenseLookup(expenseTier.expenseStore)
        val expensePaymentOperations = ExpensePaymentOperations(
            expenseTier, expenseEvents, expenseRowResolver, expenseResponseBuilder, expenseLookup
        )
        val expenseOperations = ExpenseOperations(
            expenseTier, expenseEvents, expenseRowResolver, expenseResponseBuilder, expenseLookup, expensePaymentOperations
        )
        val expenseReadOperations = ExpenseReadOperations(expenseTier, expenseResponseBuilder, expenseLookup)
        val expenseReissueOperations = ExpenseReissueOperations(expenseTier, expenseEvents, expenseLookup)
        return TierExpenseOperations(expenseOperations, expensePaymentOperations, expenseReadOperations, expenseReissueOperations)
    }
}

data class TierExpenseOperations(
    val expenseOperations: ExpenseOperations,
    val expensePaymentOperations: ExpensePaymentOperations,
    val expenseReadOperations: ExpenseReadOperations,
    val expenseReissueOperations: ExpenseReissueOperations
)
