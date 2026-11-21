package app.myliberty.easy_records.organizations.business.core.loan

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanRepository : JpaRepository<LoanEntity, UUID> {
    fun findByReferenceNumber(referenceNumber: String): LoanEntity?

    fun findTop10ByAssetAccountCodeOrderByCreatedOnDesc(assetAccountCode: String): List<LoanEntity>
}
