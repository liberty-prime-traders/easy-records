package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole

data class PermissionResponse(
    val permission: EsrPermission,
    val role: EsrRole
)
