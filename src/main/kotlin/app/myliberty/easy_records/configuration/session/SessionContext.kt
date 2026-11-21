package app.myliberty.easy_records.configuration.session

import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import java.util.UUID

data class SessionContext(
    var systemUserId: UUID? = null,
    var tenantFilterIsComplete: Boolean = false,
    var organization: OrgSession? = null,
    var location: LocationSession? = null,
    var roles: Set<EsrRole> = emptySet(),
    var permissions: Set<EsrPermission> = emptySet()
)
