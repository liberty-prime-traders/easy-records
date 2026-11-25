package app.myliberty.easy_records.platform.business.cache_admin.api

import app.myliberty.easy_records.configuration.cache.TenantCacheEvictor
import app.myliberty.easy_records.configuration.cache.TenantCachesClearedEvent
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class CacheClearService(
    private val organizationService: OrganizationService,
    private val locationService: LocationService,
    private val tenantCacheEvictor: TenantCacheEvictor,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun clearCaches(cacheClearRequestDto: CacheClearRequestDto): CacheClearResponseDto {
        val selectedIds = cacheClearRequestDto.organizationIds.orEmpty()
        val organizations = organizationService.getAllOrganizationDtos()
            .filter { selectedIds.isEmpty() || it.id in selectedIds }
            .filter { it.schemaName != null }

        val previousOrganizationSession = SessionContextProvider.getSession().organization
        try {
            val schemaNames = organizations.flatMap { clearOrganization(it) }
            return CacheClearResponseDto(
                organizationsCleared = organizations.size,
                schemasCleared = schemaNames.size,
                entriesEvicted = tenantCacheEvictor.evictSchemas(schemaNames)
            )
        } finally {
            SessionContextProvider.getSession().organization = previousOrganizationSession
        }
    }

    private fun clearOrganization(organization: OrganizationDto): List<String> {
        SessionContextProvider.initOrganization(organization)
        val locationSchemaNames = locationService.getAllLocationDtos().mapNotNull { it.schemaName }
        applicationEventPublisher.publishEvent(TenantCachesClearedEvent(organization.id))
        return listOf(organization.schemaName!!) + locationSchemaNames
    }
}
