package app.myliberty.easy_records.configuration.session

import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole
import java.util.UUID

data class SessionContext(
    var systemUserId: UUID? = null,
    var tenantFilterIsComplete: Boolean = false,
    var organization: OrgSession? = null,
    var location: LocationSession? = null,
    var roles: Set<RtsRole> = emptySet(),
    var permissions: Set<RtsPermission> = emptySet()
)
