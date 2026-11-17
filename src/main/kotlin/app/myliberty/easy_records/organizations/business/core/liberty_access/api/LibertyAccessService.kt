package app.myliberty.easy_records.organizations.business.core.liberty_access.api

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.util.enums.RtsPermission
import org.springframework.stereotype.Service

@Service
class LibertyAccessService {

    fun getLibertyPermissionsOfCurrentUser(): List<RtsPermission> =
        SessionContextProvider.getSession().permissions
            .filter { it in LIBERTY_PERMISSIONS }

    companion object {
        private val LIBERTY_PERMISSIONS = setOf(
            RtsPermission.LIBERTY_ADMIN,
            RtsPermission.MANAGE_ORGANIZATION_ACCESS
        )
    }
}
