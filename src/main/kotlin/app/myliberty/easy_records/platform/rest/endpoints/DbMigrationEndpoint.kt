package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationHistoryService
import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationRequestDto
import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationRetryRequestDto
import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationService
import app.myliberty.easy_records.platform.business.db_migration.api.OrganizationMigrationResponseDto
import org.springframework.format.annotation.DateTimeFormat
import app.myliberty.easy_records.util.enums.EsrRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/secured/db-migrations")
@PreAuthorize("hasRole('${EsrRoleNames.PLATFORM_ADMIN}')")
class DbMigrationEndpoint(
    private val dbMigrationService: DbMigrationService,
    private val dbMigrationHistoryService: DbMigrationHistoryService
) {
    @PostMapping("run")
    fun runMigration(@RequestBody dbMigrationRequestDto: DbMigrationRequestDto): OrganizationMigrationResponseDto =
        dbMigrationService.runSchemaMigration(dbMigrationRequestDto)

    @PostMapping("retry")
    fun retryFailedLocations(@RequestBody request: DbMigrationRetryRequestDto): OrganizationMigrationResponseDto {
        return dbMigrationService.retryFailedLocationMigrations(request)
    }

    @GetMapping
    fun getMigrations(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: OffsetDateTime
    ): Collection<OrganizationMigrationResponseDto> =
        dbMigrationHistoryService.getMigrationHistory(start, end)
}
