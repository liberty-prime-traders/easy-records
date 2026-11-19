package app.myliberty.easy_records.organizations.business.core.loan

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanRepaymentVoidRepository : JpaRepository<LoanRepaymentVoidEntity, UUID> {
    fun existsByLoanRepaymentId(loanRepaymentId: UUID): Boolean

    fun findByLoanRepaymentIdIn(loanRepaymentIds: Collection<UUID>): List<LoanRepaymentVoidEntity>
}
