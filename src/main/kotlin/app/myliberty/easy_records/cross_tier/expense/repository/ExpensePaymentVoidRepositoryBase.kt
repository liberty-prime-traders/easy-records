package app.myliberty.easy_records.cross_tier.expense.repository

import app.myliberty.easy_records.cross_tier.expense.entities.ExpensePaymentVoidBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpensePaymentVoidRepositoryBase<PAYMENT_VOID : ExpensePaymentVoidBase> : JpaRepository<PAYMENT_VOID, UUID> {
    fun findByExpensePaymentIdIn(expensePaymentIds: Collection<UUID>): List<PAYMENT_VOID>
}
