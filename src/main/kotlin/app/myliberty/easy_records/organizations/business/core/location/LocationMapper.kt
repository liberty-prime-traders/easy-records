package app.myliberty.easy_records.organizations.business.core.location

import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationInsertDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(locationInsertDto: LocationInsertDto): LocationEntity

    fun toDomainDto(locationEntity: LocationEntity): LocationDto

    fun toEntity(locationDto: LocationDto): LocationEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toResponseDto(locationDto: LocationDto): LocationResponseDto
}
