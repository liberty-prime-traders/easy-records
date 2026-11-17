package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserWithProfileDto
import app.myliberty.easy_records.util.enums.RtsRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
@RequestMapping("secured/platform-users")
class PlatformUserEndpoint(private val userService: SysUserService) {

    @GetMapping
    fun getPlatformUsers(): Collection<SysUserWithProfileDto> = userService.getEndUsers()
}
