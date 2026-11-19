package app.myliberty.easy_records.organizations.business.core.feature.activators

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureCache
import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureStatus
import app.myliberty.easy_records.platform.business.feature.api.Feature
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class TaxConfigurationActivator(
    private val featureCache: OrganizationFeatureCache,
) : FeatureActivator {

    override val feature = Feature.TAX_CONFIGURATION

    override fun getUnmetPrerequisites(): Set<String> {
        val hasAccounting = featureCache.getAll().any {
            it.feature == Feature.CHART_OF_ACCOUNTS && it.status == OrganizationFeatureStatus.ACTIVE
        }
        return if (hasAccounting)
            emptySet()
        else
            setOf("'Tax Configuration' cannot be activated unless 'Chart of Accounts' is active")
    }
}
