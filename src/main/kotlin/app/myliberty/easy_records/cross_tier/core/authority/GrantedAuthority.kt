package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import java.time.OffsetDateTime

data class GrantedRole(
    val role: EsrRole,
    val grantedAt: OffsetDateTime
)

data class GrantedPermission(
    val permission: EsrPermission,
    val grantedAt: OffsetDateTime
)
