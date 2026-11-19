package app.myliberty.easy_records.organizations.business.core.address

import app.myliberty.easy_records.organizations.business.core.address.api.AddressInsertDto
import app.myliberty.easy_records.organizations.business.core.address.api.AddressResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
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
