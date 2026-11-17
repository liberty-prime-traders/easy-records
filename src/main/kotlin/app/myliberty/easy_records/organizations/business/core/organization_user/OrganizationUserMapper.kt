package app.myliberty.easy_records.organizations.business.core.organization_user

import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserInsertDto
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface OrganizationUserMapper {

    fun toDomainDto(organizationUserEntity: OrganizationUserEntity): OrganizationUserDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: OrganizationUserInsertDto): OrganizationUserEntity

    fun toEntity(dto: OrganizationUserDto): OrganizationUserEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toDto(dto: OrganizationUserDto): OrganizationUserResponseDto
}
