package app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgJurisdictionTaxTypeRepository : JpaRepository<OrgJurisdictionTaxTypeEntity, UUID>
