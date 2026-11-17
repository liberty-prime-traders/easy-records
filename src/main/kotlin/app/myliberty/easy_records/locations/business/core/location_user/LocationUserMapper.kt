package app.myliberty.easy_records.locations.business.core.location_user

import app.myliberty.easy_records.locations.business.core.location_user.api.LocationUserInsertDto
import app.myliberty.easy_records.locations.business.core.location_user.api.LocationUserResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationUserMapper {

    fun toDomainDto(locationUserEntity: LocationUserEntity): LocationUserDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: LocationUserInsertDto): LocationUserEntity

    fun toEntity(locationUserDto: LocationUserDto): LocationUserEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toDto(locationUserDto: LocationUserDto): LocationUserResponseDto
}
