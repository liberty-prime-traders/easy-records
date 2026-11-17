package app.myliberty.easy_records.platform.business.organization_join_request

import app.myliberty.easy_records.platform.business.organization.api.OrganizationResponseDto
import app.myliberty.easy_records.platform.business.organization_join_request.api.OrganizationAdminJoinRequestResponseDto
import app.myliberty.easy_records.platform.business.organization_join_request.api.OrganizationJoinRequestInsertDto
import app.myliberty.easy_records.platform.business.organization_join_request.api.OrganizationJoinRequestResponseDto
import app.myliberty.easy_records.platform.business.organization_join_request.api.OrganizationLaunchResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.NullValueMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
interface OrganizationJoinRequestMapper {

    fun toDomainDto(organizationJoinRequestEntity: OrganizationJoinRequestEntity): OrganizationJoinRequestDto

    fun toEntity(dto: OrganizationJoinRequestDto): OrganizationJoinRequestEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(insertDto: OrganizationJoinRequestInsertDto): OrganizationJoinRequestEntity

    fun toLaunchResponse(
        organization: OrganizationResponseDto?,
        isOrganizationAdmin: Boolean,
        accessRequested: Boolean
    ): OrganizationLaunchResponseDto

    @Mapping(source = "subdomain", target = "domain")
    @Mapping(source = "createdOn", target = "requestedDate")
    fun toDto(dto: OrganizationJoinRequestDto): OrganizationJoinRequestResponseDto

    @Mapping(source = "createdById", target = "fullName", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "createdOn", target = "requestedDate")
    fun toAdminDto(dto: OrganizationJoinRequestDto): OrganizationAdminJoinRequestResponseDto

}
