package app.myliberty.easy_records.organizations.business.org_table_registry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgTableRegistryRepository: JpaRepository<OrgTableRegistryEntity, UUID>
