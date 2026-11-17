package app.myliberty.easy_records.configuration.filters

import jakarta.servlet.http.HttpServletRequest
import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.configuration.security.TenantHeaders.LOCATION_ID_HEADER
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@Component
class LocationSchemaInitializer(
    private val locationService: LocationService,
    @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER)
    private val organizationTransactionManager: JpaTransactionManager
) {
    fun initialize(httpServletRequest: HttpServletRequest) {
        if (SessionContextProvider.getSession().organization == null) return
        val organizationStatus = organizationTransactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            doInitialize(httpServletRequest)
        } finally {
            organizationTransactionManager.commit(organizationStatus)
        }
    }

    private fun doInitialize(httpServletRequest: HttpServletRequest) {
        httpServletRequest.getHeader(LOCATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let { UUID.fromString(it) }
            ?.let { locationId -> locationService.getAllLocationDtos().find { it.id == locationId } }
            ?.let { SessionContextProvider.initLocation(it) }
    }
}
