package app.myliberty.easy_records.cross_tier.expense.repository

import app.myliberty.easy_records.cross_tier.expense.entities.ExpenseVoidBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseVoidRepositoryBase<EXPENSE_VOID : ExpenseVoidBase> : JpaRepository<EXPENSE_VOID, UUID> {
    fun findByExpenseIdIn(expenseIds: Collection<UUID>): List<EXPENSE_VOID>
}
