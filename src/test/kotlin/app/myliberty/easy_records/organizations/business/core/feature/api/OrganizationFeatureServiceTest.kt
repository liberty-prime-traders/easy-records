package app.myliberty.easy_records.organizations.business.core.feature.api

import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureCache
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureDto
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureMapper
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureStatus
import app.myliberty.easy_records.organizations.business.core.feature.activators.FeatureActivatorRegistry
import app.myliberty.easy_records.organizations.business.core.feature.activators.TaxConfigurationActivator
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class OrganizationFeatureServiceTest {

    private val featureCache = mock(OrganizationFeatureCache::class.java)
    private val activatorRegistry = mock(FeatureActivatorRegistry::class.java)
    private val mapper = mock(OrganizationFeatureMapper::class.java)
    private val service = OrganizationFeatureService(featureCache, activatorRegistry, mapper)

    @BeforeEach
    fun setUpSession() {
        SessionContextProvider.setSession(SessionContext(systemUserId = UUID.randomUUID()))
    }

    @Test
    fun `activating a feature whose prerequisite is unmet throws and saves nothing`() {
        `when`(featureCache.getAll()).thenReturn(listOf(chartOfAccountsRow(OrganizationFeatureStatus.INACTIVE)))
        `when`(activatorRegistry.getHandler(Feature.TAX_CONFIGURATION))
            .thenReturn(listOf(TaxConfigurationActivator(featureCache)))

        val thrown = assertThrows(LibertyGenericException::class.java) {
            service.activate(hashSetOf(Feature.TAX_CONFIGURATION))
        }

        assertEquals("Cannot activate feature TAX_CONFIGURATION", thrown.message)
        verify(featureCache, never()).saveAll(anyList())
    }

    @Test
    fun `deactivating a feature with no row saves nothing`() {
        `when`(featureCache.getAll()).thenReturn(emptyList())

        service.deactivate(hashSetOf(Feature.TAX_CONFIGURATION))

        verify(featureCache, never()).saveAll(anyList())
    }

    @Test
    fun `deactivating a feature that is already inactive saves nothing`() {
        `when`(featureCache.getAll()).thenReturn(listOf(chartOfAccountsRow(OrganizationFeatureStatus.INACTIVE)))

        service.deactivate(hashSetOf(Feature.CHART_OF_ACCOUNTS))

        verify(featureCache, never()).saveAll(anyList())
    }

    private fun chartOfAccountsRow(status: OrganizationFeatureStatus) = OrganizationFeatureDto(
        feature = Feature.CHART_OF_ACCOUNTS,
        status = status
    )
}
