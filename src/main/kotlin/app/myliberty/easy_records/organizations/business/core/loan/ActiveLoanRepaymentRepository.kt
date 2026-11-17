package app.myliberty.easy_records.organizations.business.loan

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActiveLoanRepaymentRepository : JpaRepository<ActiveLoanRepaymentEntity, UUID> {

    fun findByLoanIdOrderByCreatedOnAscIdAsc(loanId: UUID): List<ActiveLoanRepaymentEntity>
}
