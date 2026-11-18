package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.cross_tier.core.authority.Authority
import app.myliberty.easy_records.cross_tier.core.authority.AuthorityHolderResponse
import app.myliberty.easy_records.cross_tier.core.authority.AuthorityType
import app.myliberty.easy_records.cross_tier.core.authority.AuthorizationCatalogService
import app.myliberty.easy_records.cross_tier.core.authority.PermissionResponse
import app.myliberty.easy_records.platform.business.authority.api.PlatformAuthorityHolderService
import app.myliberty.easy_records.util.enums.EsrPermissionNames
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.EsrRoleNames
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/authorities")
class AuthorityCatalogEndpoint(
    private val authorizationCatalogService: AuthorizationCatalogService,
    private val platformAuthorityHolderService: PlatformAuthorityHolderService
) {

    @GetMapping("per-role")
    fun getPermissionsForRole(@RequestParam role: EsrRole): List<PermissionResponse> {
        return authorizationCatalogService.getPermissionsForRole(role)
    }

    @GetMapping("platform")
    @PreAuthorize("hasRole('${EsrRoleNames.PLATFORM_ADMIN}')")
    fun getPlatformCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.PLATFORM)

    @GetMapping("platform/holders")
    @PreAuthorize("hasRole('${EsrRoleNames.PLATFORM_ADMIN}')")
    fun getPlatformAuthorityHolders(
        @RequestParam authorityName: String,
        @RequestParam authorityType: AuthorityType
    ): List<AuthorityHolderResponse> =
        platformAuthorityHolderService.getHolders(authorityName, authorityType)

    @GetMapping("organization")
    @PreAuthorize("hasAuthority('${EsrPermissionNames.MANAGE_ORGANIZATION_ACCESS}')")
    fun getOrgCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.ORGANIZATION)

    @GetMapping("location")
    @PreAuthorize("hasAuthority('${EsrPermissionNames.MANAGE_LOCATION_ACCESS}')")
    fun getLocationCatalog(): List<Authority> = authorizationCatalogService.getCatalogForTier(SchemaLevel.LOCATION)
}
