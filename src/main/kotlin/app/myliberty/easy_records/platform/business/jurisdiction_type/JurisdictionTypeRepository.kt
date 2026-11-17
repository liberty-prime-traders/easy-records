package app.myliberty.easy_records.platform.business.jurisdiction_type

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JurisdictionTypeRepository : JpaRepository<JurisdictionTypeEntity, UUID>
