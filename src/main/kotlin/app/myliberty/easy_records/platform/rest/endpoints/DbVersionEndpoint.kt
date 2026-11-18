package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.db_version.api.DbVersionResponseDto
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionService
import app.myliberty.easy_records.util.enums.EsrRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/secured/db-versions")
@PreAuthorize("hasRole('${EsrRoleNames.PLATFORM_ADMIN}')")
class DbVersionEndpoint(private val dbVersionService: DbVersionService) {

    @GetMapping
    fun getAllDbVersions(): Collection<DbVersionResponseDto> = dbVersionService.getAllDbVersions()

    @PutMapping("/{versionId}/activate")
    fun activateVersion(@PathVariable versionId: UUID): DbVersionResponseDto =
        dbVersionService.activateDbVersion(versionId)
}
