package app.myliberty.easy_records.platform.business.authority

import app.myliberty.easy_records.cross_tier.core.authority.AuthorityType
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.RtsGenericException

sealed interface PlatformAuthorityLookup {
    data class ByRole(val role: RtsRole) : PlatformAuthorityLookup
    data class ByPermission(val permission: RtsPermission) : PlatformAuthorityLookup

    companion object {
        fun of(authorityName: String, authorityType: AuthorityType): PlatformAuthorityLookup = when (authorityType) {
            AuthorityType.ROLE -> ByRole(resolvePlatformRole(authorityName))
            AuthorityType.PERMISSION -> ByPermission(resolvePlatformPermission(authorityName))
        }

        private fun resolvePlatformRole(authorityName: String): RtsRole {
            val role = runCatching { RtsRole.valueOf(authorityName) }.getOrNull()
                ?: throw RtsGenericException("$authorityName is not a known role")
            if (role.tier != SchemaLevel.PLATFORM) {
                throw RtsGenericException("$authorityName is not a platform-tier role")
            }
            return role
        }

        private fun resolvePlatformPermission(authorityName: String): RtsPermission {
            val permission = runCatching { RtsPermission.valueOf(authorityName) }.getOrNull()
                ?: throw RtsGenericException("$authorityName is not a known permission")
            if (permission.tier != SchemaLevel.PLATFORM) {
                throw RtsGenericException("$authorityName is not a platform-tier permission")
            }
            return permission
        }
    }
}
