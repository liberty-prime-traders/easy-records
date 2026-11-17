package app.myliberty.easy_records.platform.business.organization

import app.myliberty.easy_records.platform.business.db_version.api.DbVersionNumber
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationInsertDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [AuthorizationPassQualifier::class, DbVersionService::class]
)
interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "currentDbVersionId", ignore = true)
    @Mapping(target = "creationPassId", ignore = true)
    fun toEntity(dto: OrganizationInsertDto): OrganizationEntity

    fun toDomainDto(organizationEntity: OrganizationEntity): OrganizationDto

    fun toEntity(dto: OrganizationDto): OrganizationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "currentDbVersionId", target = "currentDbVersion", qualifiedBy = [DbVersionNumber::class])
    @Mapping(source = "creationPassId", target = "creationPassReferenceNumber", qualifiedBy = [PassReferenceNumber::class])
    fun toResponseDto(dto: OrganizationDto): OrganizationResponseDto
}
