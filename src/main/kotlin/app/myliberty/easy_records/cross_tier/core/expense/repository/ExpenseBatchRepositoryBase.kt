package app.myliberty.easy_records.cross_tier.core.expense.repository

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseBatchBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseBatchRepositoryBase<BATCH : ExpenseBatchBase> : JpaRepository<BATCH, UUID> {

    fun findBySourceTypeAndSourceReference(sourceType: ExpenseSourceType, sourceReference: String): BATCH?
}
