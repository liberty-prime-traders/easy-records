package app.myliberty.easy_records.cross_tier.authority

import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole

data class PermissionResponse(
    val permission: RtsPermission,
    val role: RtsRole
)
