package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.cross_tier.core.authority.MembershipUserSummary
import app.myliberty.easy_records.cross_tier.core.authority.UserAccessDetail
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserResponseDto
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.util.enums.EsrPermissionNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/organization-users")
class OrganizationUserEndpoint(private val organizationUserService: OrganizationUserService) {

    @GetMapping
    @PreAuthorize("hasAuthority('${EsrPermissionNames.VIEW_USERS_OF_ORG}')")
    fun getOrganizationUsers(): Collection<MembershipUserSummary> {
        return organizationUserService.getOrganizationUserSummaries()
    }

    @GetMapping("access")
    @PreAuthorize("hasAuthority('${EsrPermissionNames.VIEW_USERS_OF_ORG}')")
    fun getUserAccess(@RequestParam userId: UUID): UserAccessDetail {
        return organizationUserService.getUserAccessDetail(userId)
    }

    @PostMapping("terminate")
    @PreAuthorize("hasAuthority('${EsrPermissionNames.REMOVE_USER_FROM_ORG}')")
    fun terminateUsers(@RequestBody organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        return organizationUserService.terminateOrganizationUsers(organizationUserIds)
    }
}
