package app.myliberty.easy_records.organizations.business.organization_admin

import app.myliberty.easy_records.organizations.business.organization_admin.api.OrganizationAdminInsertDto
import app.myliberty.easy_records.organizations.business.organization_admin.api.OrganizationAdminResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationAdminMapper {

    fun toDomainDto(organizationAdminEntity: OrganizationAdminEntity): OrganizationAdminDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: OrganizationAdminInsertDto): OrganizationAdminEntity

    fun toEntity(dto: OrganizationAdminDto): OrganizationAdminEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toResponseDto(dto: OrganizationAdminDto): OrganizationAdminResponseDto
}
