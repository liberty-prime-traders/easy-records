package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthorizationBatchExecutor(
    private val roleGrantService: RoleGrantService,
    private val permissionGrantService: PermissionGrantService
) {

    @TransactionalOnPlatformSchema
    fun assignAtPlatformTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnOrganizationSchema
    fun assignAtOrganizationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnLocationSchema
    fun assignAtLocationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::assignRoles, permissionGrantService::assignPermissions)

    @TransactionalOnPlatformSchema
    fun removeAtPlatformTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    @TransactionalOnOrganizationSchema
    fun removeAtOrganizationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    @TransactionalOnLocationSchema
    fun removeAtLocationTier(request: AuthorizationAssignmentRequest) =
        apply(request, roleGrantService::removeRoles, permissionGrantService::removePermissions)

    private fun apply(
        request: AuthorizationAssignmentRequest,
        roleAction: (Collection<UUID>, Collection<RtsRole>) -> Unit,
        permissionAction: (Collection<UUID>, Collection<RtsPermission>) -> Unit
    ) {
        roleAction(request.userIds, request.roles)
        permissionAction(request.userIds, request.permissions)
    }
}
