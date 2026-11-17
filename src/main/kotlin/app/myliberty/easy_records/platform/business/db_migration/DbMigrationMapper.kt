package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationInsertDto
import app.myliberty.easy_records.platform.business.db_migration.api.LocationMigrationResponse
import app.myliberty.easy_records.platform.business.db_migration.api.OrganizationMigrationResponseDto
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionNumber
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionService
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = LibertyMapperConfig::class,
    uses = [DbVersionService::class, DbMigrationQualifier::class]
)
interface DbMigrationMapper {
    fun toDomainDto(dbMigrationEntity: DbMigrationEntity): DbMigrationDto

    fun toEntity(dto: DbMigrationDto): DbMigrationEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: DbMigrationInsertDto): DbMigrationEntity

    @Mapping(target = "locations", ignore = true)
    @Mapping(source = ".", target = "organizationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toOrganizationResponseDto(dto: DbMigrationDto): OrganizationMigrationResponseDto

    @Mapping(target = "locationId", source = "schemaOwnerId")
    @Mapping(source = ".", target = "locationName", qualifiedBy = [SchemaOwnerName::class])
    @Mapping(source = "dbVersionId", target = "versionNumber", qualifiedBy = [DbVersionNumber::class])
    fun toLocationResponseDto(dto: DbMigrationDto): LocationMigrationResponse
}
