package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole
import java.time.OffsetDateTime

data class GrantedRole(
    val role: RtsRole,
    val grantedAt: OffsetDateTime
)

data class GrantedPermission(
    val permission: RtsPermission,
    val grantedAt: OffsetDateTime
)
