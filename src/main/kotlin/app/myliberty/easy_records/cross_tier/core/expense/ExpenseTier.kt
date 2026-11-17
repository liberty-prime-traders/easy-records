package app.myliberty.easy_records.cross_tier.core.expense

import app.myliberty.easy_records.cross_tier.core.expense.store.ExpenseStore
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import java.util.UUID

interface ExpenseTier {

    val expenseStore: ExpenseStore

    fun sourceContext(): EventSourceContext

    fun lockExpense(expenseId: UUID)

    fun lockExpenses(expenseIds: Collection<UUID>)

    fun lockSourceDocument(sourceDocumentId: UUID)
}
