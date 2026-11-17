package app.myliberty.easy_records.organizations.business.feature.activators

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.account.api.CoaDefaultsInserter
import app.myliberty.easy_records.organizations.business.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class AccountingFeatureActivator(
    private val coaDefaultsInserter: CoaDefaultsInserter,
    private val orgAccountingConfigService: OrgAccountingConfigService
) : FeatureActivator {

    override val feature = Feature.CHART_OF_ACCOUNTS

    override fun onActivate() {
        coaDefaultsInserter.seed()
        orgAccountingConfigService.initialize()
    }
}
