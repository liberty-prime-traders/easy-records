package app.myliberty.easy_records.organizations.business.feature

import app.myliberty.easy_records.organizations.business.feature.api.OrganizationFeatureResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrganizationFeatureMapper {
    fun toDomainDto(organizationFeatureEntity: OrganizationFeatureEntity): OrganizationFeatureDto
    fun toEntity(dto: OrganizationFeatureDto): OrganizationFeatureEntity

    @Mapping(target = "enabledBy", source = "enabledBy", qualifiedBy = [NullableFullName::class])
    @Mapping(target = "disabledBy", source = "disabledBy", qualifiedBy = [NullableFullName::class])
    fun toResponseDto(dto: OrganizationFeatureDto): OrganizationFeatureResponseDto
}
