package app.myliberty.easy_records.configuration.session

import app.myliberty.easy_records.configuration.security.RtsHeaders
import app.myliberty.easy_records.organizations.business.location.api.LocationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.exceptions.RtsMissingHeaderException
import java.util.UUID

object SessionContextProvider {
    private val sessionContextThreadLocal = ThreadLocal<SessionContext?>()

    fun setSession(sessionContext: SessionContext) {
        sessionContextThreadLocal.set(sessionContext)
    }

    fun getSession(): SessionContext {
        return sessionContextThreadLocal.get() ?: SessionContext().also { sessionContextThreadLocal.set(it) }
    }

    fun getUserId(): UUID {
        return getSession().systemUserId ?: throw RtsGenericException("User ID not found in session")
    }

    fun getUserIdOrNull(): UUID? {
        return getSession().systemUserId
    }

    fun getOrganizationId(): UUID {
        return getSession().organization?.id ?: throw RtsMissingHeaderException(RtsHeaders.ORGANIZATION_ID_HEADER)
    }

    fun getOrganizationIdOrNull(): UUID? {
        return getSession().organization?.id
    }

    fun getLocationId(): UUID {
        return getSession().location?.id ?: throw RtsMissingHeaderException(RtsHeaders.LOCATION_ID_HEADER)
    }

    fun getLocationIdOrNull(): UUID? {
        return getSession().location?.id
    }

    fun getLocationSchema(): String {
        return getSession().location?.schemaName ?: throw RtsGenericException("Location schema not found in session.")
    }

    fun getOrganizationSchema(): String {
        return getSession().organization?.schemaName ?: throw RtsGenericException("Organization schema not found in session.")
    }

    fun getOrgTimezone(): String {
        return getSession().organization?.timezone ?: throw RtsGenericException("Organization timezone not found in session.")
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }

    fun initOrganization(organization: OrganizationDto) {
        getSession().organization = OrgSession(
            id = organization.id,
            schemaName = organization.schemaName!!,
            timezone = organization.timezone
        )
    }

    fun initLocation(location: LocationDto) {
        getSession().location = LocationSession(
            id = location.id,
            schemaName = location.schemaName!!,
            timezone = location.timezone
        )
    }

    fun initSystemUser(userId: UUID) {
        getSession().systemUserId = userId
    }
}
