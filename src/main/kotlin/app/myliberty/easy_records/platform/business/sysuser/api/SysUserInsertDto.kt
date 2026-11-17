package app.myliberty.easy_records.platform.business.sysuser.api

import java.io.Serializable

data class SysUserInsertDto(
    val email: String? = null,
    val localFirstName: String,
    val localLastName: String? = null,
    val userType: UserType? = null
) : Serializable
