package app.myliberty.easy_records.organizations.business.core.organization_admin.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.organization_admin.OrganizationAdminCache
import app.myliberty.easy_records.organizations.business.core.organization_admin.OrganizationAdminMapper
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationAdminService(
    private val organizationAdminMapper: OrganizationAdminMapper,
    private val organizationAdminCache: OrganizationAdminCache,
    private val sysUserService: SysUserService,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAdminHistory(): Collection<OrganizationAdminResponseDto> {
        return organizationAdminCache.getAdminHistory()
            .map { organizationAdminMapper.toResponseDto(it) }
    }

    fun createOrganizationAdmin(adminId: UUID): OrganizationAdminResponseDto {
        validateUserExists(adminId)
        val dto = organizationAdminCache.create(OrganizationAdminInsertDto(userId = adminId))
        return organizationAdminMapper.toResponseDto(dto)
    }

    private fun validateUserExists(adminId: UUID) {
        sysUserService.findById(adminId)
            ?: throw LibertyGenericException("User with id $adminId not found")
    }

    fun terminateOrganizationAdmin(adminId: UUID) {
        organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == adminId }
            ?.let { organizationAdminCache.save(it.copy(endOn = OffsetDateTime.now())) }
    }

    fun isOrganizationAdmin(): Boolean {
        val userId = SessionContextProvider.getUserId()
        return organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == userId } != null
    }

    fun registerFounder(userId: UUID) {
        organizationAdminCache.create(OrganizationAdminInsertDto(userId = userId))
    }
}
