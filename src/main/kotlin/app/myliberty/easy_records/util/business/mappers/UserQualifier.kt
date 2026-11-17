package app.myliberty.easy_records.util.business.mappers

import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserQualifier(private val sysUserService: SysUserService) {

    @NullableFullName
    fun getUserFullName(userId: UUID?): String? {
        if (userId == null) return null
        return findUser(userId)?.fullName
    }

    @CreatorFullName
    fun getCreatorFullName(createdById: UUID): String {
        return findUser(createdById)?.fullName ?: DELETED_USER_LABEL
    }

    private fun findUser(userId: UUID) = sysUserService.getUsersById()[userId]

    companion object {
        const val DELETED_USER_LABEL = "Deleted User"
    }
}
