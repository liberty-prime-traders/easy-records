package app.myliberty.easy_records.organizations.business.core.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubledgerBalanceRepository : JpaRepository<SubledgerBalanceEntity, UUID> {
    fun findByContactReferenceNumberIn(contactReferenceNumbers: Set<String>): List<SubledgerBalanceEntity>
}
