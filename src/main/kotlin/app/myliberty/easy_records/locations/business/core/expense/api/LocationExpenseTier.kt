package app.myliberty.easy_records.locations.business.core.expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseTier
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class LocationExpenseTier(
    override val expenseStore: LocationExpenseStore,
    private val entityAdvisoryLock: EntityAdvisoryLock
) : ExpenseTier {

    override fun sourceContext(): EventSourceContext = EventSourceContext.LocationLevel(
        orgSchema = SessionContextProvider.getOrganizationSchema(),
        locationSchema = SessionContextProvider.getLocationSchema()
    )

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpenses(expenseIds: Collection<UUID>) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseIds)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId)
    }
}
