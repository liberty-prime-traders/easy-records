package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.organizations.business.core.liberty_access.api.LibertyAccessService
import app.myliberty.easy_records.util.enums.EsrPermission
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/liberty-permissions")
class LibertyAccessEndpoint(private val libertyAccessService: LibertyAccessService) {

    @GetMapping
    @PreAuthorize("permitAll()")
    fun getLibertyPermissionsOfCurrentUser(): List<EsrPermission> =
        libertyAccessService.getLibertyPermissionsOfCurrentUser()
}
