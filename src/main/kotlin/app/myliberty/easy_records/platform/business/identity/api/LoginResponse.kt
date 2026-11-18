package app.myliberty.easy_records.platform.business.identity.api

import app.myliberty.easy_records.platform.business.sysuser.api.SysUserWithProfileDto
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole

data class LoginResponse(
    val sessionToken: String,
    val user: SysUserWithProfileDto,
    val verifiedRoles: List<EsrRole> = emptyList(),
    val verifiedPermissions: List<EsrPermission> = emptyList()
)
