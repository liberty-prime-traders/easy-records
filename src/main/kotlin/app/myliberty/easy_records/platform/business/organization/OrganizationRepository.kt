package app.myliberty.easy_records.platform.business.organization

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository: JpaRepository<OrganizationEntity, UUID> {
    fun findOneBySubdomain(subdomain: String): OrganizationEntity?
}
