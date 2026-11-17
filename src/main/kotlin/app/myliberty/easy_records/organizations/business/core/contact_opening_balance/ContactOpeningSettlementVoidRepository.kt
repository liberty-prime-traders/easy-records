package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContactOpeningSettlementVoidRepository : JpaRepository<ContactOpeningSettlementVoidEntity, UUID> {

    fun existsBySettlementId(settlementId: UUID): Boolean

    fun findBySettlementIdIn(settlementIds: Collection<UUID>): List<ContactOpeningSettlementVoidEntity>
}
