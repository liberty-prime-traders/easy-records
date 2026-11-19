package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import app.myliberty.easy_records.platform.business.role_assignment.api.PlatformRoleAssignmentService
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.stereotype.Service

@Service
class AuthorizationCatalogService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService
) {

    fun getCatalogForTier(tier: SchemaLevel): List<Authority> = catalogFor(
        tier,
        EsrRole.entries.filter { it.tier == tier },
        EsrPermission.entries.filter { it.tier == tier }
    )

    fun getPermissionsForRole(role: EsrRole): List<PermissionResponse> =
        role.permissions.map { PermissionResponse(it, role) }

    private fun catalogFor(tier: SchemaLevel, roles: List<EsrRole>, permissions: List<EsrPermission>): List<Authority> {
        val roleAuthorities = roles.map {
            Authority(it.name, it.tier, AuthorityType.ROLE, holderCountOf(tier, it))
        }
        val permissionAuthorities = permissions.map {
            Authority(it.name, it.tier, AuthorityType.PERMISSION, holderCountOf(tier, it))
        }
        return roleAuthorities + permissionAuthorities
    }

    private fun holderCountOf(tier: SchemaLevel, role: EsrRole): Int? =
        if (tier == SchemaLevel.PLATFORM) platformRoleAssignmentService.getHolders(role).size else null

    private fun holderCountOf(tier: SchemaLevel, permission: EsrPermission): Int? =
        if (tier == SchemaLevel.PLATFORM) {
            platformPermissionAssignmentService.getHolders(permission).size
        } else {
            null
        }
}
