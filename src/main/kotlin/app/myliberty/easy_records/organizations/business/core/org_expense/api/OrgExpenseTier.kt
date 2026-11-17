package app.myliberty.easy_records.organizations.business.org_expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.expense.ExpenseTier
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.organizations.business.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class OrgExpenseTier(
    override val expenseStore: OrgExpenseStore,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) : ExpenseTier {

    override fun sourceContext(): EventSourceContext =
        EventSourceContext.OrgLevel(orgSchema = SessionContextProvider.getOrganizationSchema())

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId.toString())
    }

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpenses(expenseIds: Collection<UUID>) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseIds.map { it.toString() })
    }

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId.toString())
    }
}
