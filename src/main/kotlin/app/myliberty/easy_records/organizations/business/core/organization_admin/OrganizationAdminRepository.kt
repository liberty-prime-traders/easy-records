package app.myliberty.easy_records.organizations.business.core.organization_admin

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationAdminRepository: JpaRepository<OrganizationAdminEntity, UUID>
