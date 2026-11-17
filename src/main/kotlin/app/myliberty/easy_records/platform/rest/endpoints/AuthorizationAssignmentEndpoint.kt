package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.cross_tier.core.authority.AuthorizationAssignmentRequest
import app.myliberty.easy_records.cross_tier.core.authority.AuthorizationRequestService
import app.myliberty.easy_records.util.enums.RtsPermissionNames
import app.myliberty.easy_records.util.enums.RtsRoleNames
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@ResponseStatus(HttpStatus.OK)
@RequestMapping("secured/authorizations")
class AuthorizationAssignmentEndpoint(private val authorizationRequestService: AuthorizationRequestService) {

    @PostMapping("platform")
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun assignAtPlatformTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.assignAtPlatformTier(authorizationAssignmentRequest)
    }

    @DeleteMapping("platform")
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun removeAtPlatformTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.removeAtPlatformTier(authorizationAssignmentRequest)
    }

    @PostMapping("organization")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_ORGANIZATION_ACCESS}')")
    fun assignAtOrganizationTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.assignAtOrganizationTier(authorizationAssignmentRequest)
    }

    @DeleteMapping("organization")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_ORGANIZATION_ACCESS}')")
    fun removeAtOrganizationTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.removeAtOrganizationTier(authorizationAssignmentRequest)
    }

    @PostMapping("location")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_LOCATION_ACCESS}')")
    fun assignAtLocationTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.assignAtLocationTier(authorizationAssignmentRequest)
    }

    @DeleteMapping("location")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.MANAGE_LOCATION_ACCESS}')")
    fun removeAtLocationTier(@RequestBody authorizationAssignmentRequest: AuthorizationAssignmentRequest) {
        authorizationRequestService.removeAtLocationTier(authorizationAssignmentRequest)
    }
}
