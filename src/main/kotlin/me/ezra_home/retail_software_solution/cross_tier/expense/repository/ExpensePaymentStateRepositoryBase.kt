package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpensePaymentStateRepositoryBase<STATE : ExpensePaymentStateBase> : JpaRepository<STATE, UUID> {

    fun findByExpenseIdIn(expenseIds: Collection<UUID>): List<STATE>
}
