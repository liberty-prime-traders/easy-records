package app.myliberty.easy_records.cross_tier.core.expense.repository

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentStateBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpensePaymentStateRepositoryBase<STATE : ExpensePaymentStateBase> : JpaRepository<STATE, UUID> {

    fun findByExpenseIdIn(expenseIds: Collection<UUID>): List<STATE>
}
