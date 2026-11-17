package app.myliberty.easy_records.organizations.business.address

import app.myliberty.easy_records.organizations.business.address.api.AddressInsertDto
import app.myliberty.easy_records.organizations.business.address.api.AddressResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(addressInsertDto: AddressInsertDto): AddressEntity

    fun toDomainDto(addressEntity: AddressEntity): AddressDto

    fun toEntity(addressDto: AddressDto): AddressEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toDto(addressDto: AddressDto): AddressResponseDto
}
