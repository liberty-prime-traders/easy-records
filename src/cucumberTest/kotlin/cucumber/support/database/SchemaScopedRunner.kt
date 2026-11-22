package app.myliberty.easy_records.cucumber.support.database

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.organizations.business.core.location.LocationCache
import app.myliberty.easy_records.platform.business.organization.OrganizationCache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

// A repository only reaches organization and location tables inside a transaction on that schema's manager, with the
// tenant in the session. The entity's package decides the schema.
@Component
class SchemaScopedRunner(
  private val organizationCache: OrganizationCache,
  private val locationCache: LocationCache,
  private val injectContext: InjectContext,

  @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
  private val platformTransactionManager: PlatformTransactionManager,

  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER)
  private val organizationTransactionManager: PlatformTransactionManager,

  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_TRANSACTION_MANAGER)
  private val locationTransactionManager: PlatformTransactionManager,
) {

  fun <T> run(entityClass: Class<*>, block: () -> T): T {
    val packageName = entityClass.packageName
    return when {
      ".organizations." in packageName -> inOrganization(organizationTransactionManager, withLocation = false, block)
      ".locations." in packageName -> inOrganization(locationTransactionManager, withLocation = true, block)
      else -> TransactionTemplate(platformTransactionManager).execute { block() }!!
    }
  }

  private fun <T> inOrganization(transactionManager: PlatformTransactionManager, withLocation: Boolean, block: () -> T): T {
    val organizationId = injectContext.get(PersistentKey.ORGANIZATION)
    val organization = organizationCache.getAllOrganizations().first { it.id == organizationId }
    SessionContextProvider.initOrganization(organization)
    try {
      if (withLocation) {
        val locationId = injectContext.get(PersistentKey.LOCATION)
        SessionContextProvider.initLocation(locationCache.getAllLocations().first { it.id == locationId })
      }
      return TransactionTemplate(transactionManager).execute { block() }!!
    } finally {
      SessionContextProvider.clear()
    }
  }
}
