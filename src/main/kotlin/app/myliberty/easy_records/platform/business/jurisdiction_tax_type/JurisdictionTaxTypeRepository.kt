package app.myliberty.easy_records.platform.business.jurisdiction_tax_type

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JurisdictionTaxTypeRepository : JpaRepository<JurisdictionTaxTypeEntity, UUID>
