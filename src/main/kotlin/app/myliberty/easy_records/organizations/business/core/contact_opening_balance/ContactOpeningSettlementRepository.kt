package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContactOpeningSettlementRepository : JpaRepository<ContactOpeningSettlementEntity, UUID> {

    fun findByContactReferenceNumberOrderByCreatedOnDescIdDesc(
        contactReferenceNumber: String
    ): List<ContactOpeningSettlementEntity>
}
