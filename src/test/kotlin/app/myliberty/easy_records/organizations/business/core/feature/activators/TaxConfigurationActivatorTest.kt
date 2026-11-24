package app.myliberty.easy_records.organizations.business.core.feature.activators

import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureCache
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureDto
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureStatus
import app.myliberty.easy_records.platform.business.feature.api.Feature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TaxConfigurationActivatorTest {

    private val taxConfigurationMessage =
        "'Tax Configuration' cannot be activated unless 'Chart of Accounts' is active"

    @Test
    fun `activation is refused when no chart of accounts row exists`() {
        val featureCache = mock(OrganizationFeatureCache::class.java)
        `when`(featureCache.getAll()).thenReturn(emptyList())

        val unmetPrerequisites = TaxConfigurationActivator(featureCache).getUnmetPrerequisites()

        assertEquals(setOf(taxConfigurationMessage), unmetPrerequisites)
    }

    @Test
    fun `activation is refused when chart of accounts is inactive`() {
        val featureCache = mock(OrganizationFeatureCache::class.java)
        `when`(featureCache.getAll()).thenReturn(listOf(chartOfAccountsRow(OrganizationFeatureStatus.INACTIVE)))

        val unmetPrerequisites = TaxConfigurationActivator(featureCache).getUnmetPrerequisites()

        assertEquals(setOf(taxConfigurationMessage), unmetPrerequisites)
    }

    @Test
    fun `activation has no unmet prerequisites when chart of accounts is active`() {
        val featureCache = mock(OrganizationFeatureCache::class.java)
        `when`(featureCache.getAll()).thenReturn(listOf(chartOfAccountsRow(OrganizationFeatureStatus.ACTIVE)))

        val unmetPrerequisites = TaxConfigurationActivator(featureCache).getUnmetPrerequisites()

        assertTrue(unmetPrerequisites.isEmpty())
    }

    private fun chartOfAccountsRow(status: OrganizationFeatureStatus) = OrganizationFeatureDto(
        feature = Feature.CHART_OF_ACCOUNTS,
        status = status
    )
}
