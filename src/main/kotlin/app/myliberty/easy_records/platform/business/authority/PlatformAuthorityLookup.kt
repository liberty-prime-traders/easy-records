package app.myliberty.easy_records.platform.business.authority

import app.myliberty.easy_records.cross_tier.core.authority.AuthorityType
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

sealed interface PlatformAuthorityLookup {
    data class ByRole(val role: EsrRole) : PlatformAuthorityLookup
    data class ByPermission(val permission: EsrPermission) : PlatformAuthorityLookup

    companion object {
        fun of(authorityName: String, authorityType: AuthorityType): PlatformAuthorityLookup = when (authorityType) {
            AuthorityType.ROLE -> ByRole(resolvePlatformRole(authorityName))
            AuthorityType.PERMISSION -> ByPermission(resolvePlatformPermission(authorityName))
        }

        private fun resolvePlatformRole(authorityName: String): EsrRole {
            val role = runCatching { EsrRole.valueOf(authorityName) }.getOrNull()
                ?: throw LibertyGenericException("$authorityName is not a known role")
            if (role.tier != SchemaLevel.PLATFORM) {
                throw LibertyGenericException("$authorityName is not a platform-tier role")
            }
            return role
        }

        private fun resolvePlatformPermission(authorityName: String): EsrPermission {
            val permission = runCatching { EsrPermission.valueOf(authorityName) }.getOrNull()
                ?: throw LibertyGenericException("$authorityName is not a known permission")
            if (permission.tier != SchemaLevel.PLATFORM) {
                throw LibertyGenericException("$authorityName is not a platform-tier permission")
            }
            return permission
        }
    }
}
