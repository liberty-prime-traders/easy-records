package app.myliberty.easy_records.platform.business.sysuser

import app.myliberty.easy_records.platform.business.sysuser.api.UserType
import java.time.OffsetDateTime
import java.util.UUID

data class SysUserDto(
    val id: UUID,
    val createdById: UUID? = null,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val email: String? = null,
    val disabledAt: OffsetDateTime? = null,
    val localFirstName: String,
    val localLastName: String? = null,
    val userType: UserType? = null
)
