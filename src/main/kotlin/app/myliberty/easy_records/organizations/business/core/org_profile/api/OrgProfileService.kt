package app.myliberty.easy_records.organizations.business.core.org_profile.api

import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.util.async.AsyncExecutor
import app.myliberty.easy_records.util.enums.ServiceAccount
import org.springframework.stereotype.Service

@Service
class OrgProfileService(
    private val orgDataSeeders: List<OrgDataSeeder>,
    private val asyncExecutor: AsyncExecutor
) {

    fun applySeedDefaults() {
        asyncExecutor.execute {
            ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                orgDataSeeders.forEach { it.seed() }
            }
        }
    }
}
