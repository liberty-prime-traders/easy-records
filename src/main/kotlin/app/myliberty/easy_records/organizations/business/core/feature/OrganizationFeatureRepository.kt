package app.myliberty.easy_records.organizations.business.feature

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationFeatureRepository : JpaRepository<OrganizationFeatureEntity, UUID>
