package app.myliberty.easy_records.platform.business.sysuser.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.platform.business.sysuser.SysUserCache
import app.myliberty.easy_records.platform.business.sysuser.SysUserDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.enums.ServiceAccount
import app.myliberty.easy_records.util.exceptions.AuthException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SysUserService(
    private val sysUserCache: SysUserCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllUsers(): Collection<SysUserWithProfileDto> = sysUserCache.getAllUsers()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getUsersById(): Map<UUID, SysUserWithProfileDto> = sysUserCache.getUsersById()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getEndUsers(): Collection<SysUserWithProfileDto> = sysUserCache.getEndUsers()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun findByEmail(email: String): SysUserWithProfileDto? = getAllUsers().find { it.email == email }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun findById(userId: UUID): SysUserWithProfileDto? = getUsersById()[userId]

    @TransactionalOnPlatformSchema(readOnly = true)
    fun throwIfAccountIsDisabled(userId: UUID) {
        sysUserCache.getSystemUsers()
            .find { it.id == userId }?.disabledAt
            ?.run { throw AuthException.AccountDisabled() }
    }

    @TransactionalOnPlatformSchema
    fun createUser(email: String?, localFirstName: String, localLastName: String?): UUID {
        StringUtils.requireHasValue(localFirstName, "A SysUser must have a local first name")
        val created = ServiceAccountContext.runWithServiceAccount<SysUserDto>(ServiceAccount.RECORD_INITIALIZER) {
            sysUserCache.create(
                SysUserInsertDto(
                    email = email,
                    localFirstName = localFirstName,
                    localLastName = localLastName,
                    userType = UserType.END_USER
                )
            )
        }
        return created.id
    }
}
