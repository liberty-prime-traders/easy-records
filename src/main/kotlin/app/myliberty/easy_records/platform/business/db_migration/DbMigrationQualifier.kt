package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import app.myliberty.easy_records.util.enums.SchemaOwnerType
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SchemaOwnerName


@Component
class DbMigrationQualifier(
    private val organizationService: OrganizationService,
    private val locationService: LocationService
) {
    @SchemaOwnerName
    fun getSchemaOwnerName(dto: DbMigrationDto): String? {
        return when (dto.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION ->
                organizationService.getAllOrganizationDtos().find { it.id == dto.schemaOwnerId }?.name

            SchemaOwnerType.LOCATION ->
                locationService.getAllLocationDtos().find { it.id == dto.schemaOwnerId }?.name
        }
    }
}
