package app.myliberty.easy_records.configuration.filters

import jakarta.servlet.http.HttpServletRequest
import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.configuration.security.TenantHeaders.ORGANIZATION_ID_HEADER
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@Component
class OrganizationSchemaInitializer(
    private val organizationService: OrganizationService,
    @param:Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
    private val platformTransactionManager: JpaTransactionManager
) {

    fun initialize(httpServletRequest: HttpServletRequest) {
        val platformStatus = platformTransactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            doInitialize(httpServletRequest)
        } finally {
            platformTransactionManager.commit(platformStatus)
        }
    }

    private fun doInitialize(httpServletRequest: HttpServletRequest) {
        httpServletRequest.getHeader(ORGANIZATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let { UUID.fromString(it) }
            ?.let { organizationId -> organizationService.getAllOrganizationDtos().find { it.id == organizationId } }
            ?.let { SessionContextProvider.initOrganization(it) }
    }
}
