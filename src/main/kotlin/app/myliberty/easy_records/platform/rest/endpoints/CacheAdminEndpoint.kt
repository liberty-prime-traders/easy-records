package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearRequestDto
import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearResponseDto
import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearService
import app.myliberty.easy_records.util.enums.EsrRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/secured/cache-admin")
@PreAuthorize("hasRole('${EsrRoleNames.PLATFORM_ADMIN}')")
class CacheAdminEndpoint(private val cacheClearService: CacheClearService) {

    @PostMapping("clear")
    fun clearCaches(@RequestBody cacheClearRequestDto: CacheClearRequestDto): CacheClearResponseDto =
        cacheClearService.clearCaches(cacheClearRequestDto)
}
