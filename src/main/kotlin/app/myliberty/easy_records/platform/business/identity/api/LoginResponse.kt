package app.myliberty.easy_records.platform.business.identity.api

import app.myliberty.easy_records.platform.business.sysuser.api.SysUserWithProfileDto
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole

data class LoginResponse(
    val sessionToken: String,
    val user: SysUserWithProfileDto,
    val verifiedRoles: List<RtsRole> = emptyList(),
    val verifiedPermissions: List<RtsPermission> = emptyList()
)
