package app.myliberty.easy_records.platform.business.cache_admin.api

import app.myliberty.easy_records.configuration.cache.TenantCacheEvictor
import app.myliberty.easy_records.configuration.cache.TenantCachesClearedEvent
import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationEventPublisher
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class CacheClearServiceTest {

    private lateinit var organizationService: OrganizationService
    private lateinit var locationService: LocationService
    private lateinit var tenantCacheEvictor: TenantCacheEvictor
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    private lateinit var cacheClearService: CacheClearService

    private val organizationA = organization("org-a")
    private val organizationB = organization("org-b")

    private fun organization(schemaName: String) = OrganizationDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "ref", name = schemaName, schemaName = schemaName, timezone = "UTC"
    )

    private fun location(schemaName: String) = LocationDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "ref", schemaName = schemaName, timezone = "UTC"
    )

    @BeforeEach
    fun setUp() {
        organizationService = mock(OrganizationService::class.java)
        locationService = mock(LocationService::class.java)
        tenantCacheEvictor = mock(TenantCacheEvictor::class.java)
        applicationEventPublisher = mock(ApplicationEventPublisher::class.java)
        cacheClearService = CacheClearService(organizationService, locationService, tenantCacheEvictor, applicationEventPublisher)

        `when`(organizationService.getAllOrganizationDtos()).thenReturn(listOf(organizationA, organizationB))
        `when`(locationService.getAllLocationDtos()).thenReturn(listOf(location("loc-1")))
        `when`(tenantCacheEvictor.evictSchemas(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(7)
    }

    @Test
    fun `an empty request clears every organization with its location schemas`() {
        val response = cacheClearService.clearCaches(CacheClearRequestDto())

        assertEquals(CacheClearResponseDto(organizationsCleared = 2, schemasCleared = 4, entriesEvicted = 7), response)
        verify(tenantCacheEvictor).evictSchemas(listOf("org-a", "loc-1", "org-b", "loc-1"))
        verify(applicationEventPublisher).publishEvent(TenantCachesClearedEvent(organizationA.id))
        verify(applicationEventPublisher).publishEvent(TenantCachesClearedEvent(organizationB.id))
    }

    @Test
    fun `selected organizations are the only ones cleared`() {
        val response = cacheClearService.clearCaches(CacheClearRequestDto(organizationIds = setOf(organizationB.id)))

        assertEquals(CacheClearResponseDto(organizationsCleared = 1, schemasCleared = 2, entriesEvicted = 7), response)
        verify(tenantCacheEvictor).evictSchemas(listOf("org-b", "loc-1"))
        verify(applicationEventPublisher).publishEvent(TenantCachesClearedEvent(organizationB.id))
        verify(applicationEventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.any(TenantCachesClearedEvent::class.java))
    }
}
