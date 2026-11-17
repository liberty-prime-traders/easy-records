package app.myliberty.easy_records.cross_tier.core.expense.repository

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseRepositoryBase<EXPENSE : ExpenseBase> : JpaRepository<EXPENSE, UUID> {

    fun findByReferenceNumber(referenceNumber: String): EXPENSE?

    fun findByReferenceNumberIn(referenceNumbers: Collection<String>): List<EXPENSE>

    fun findByBatchId(batchId: UUID): List<EXPENSE>
}
