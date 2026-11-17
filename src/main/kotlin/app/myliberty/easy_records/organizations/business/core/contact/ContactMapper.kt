package app.myliberty.easy_records.organizations.business.contact

import app.myliberty.easy_records.organizations.business.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactInsertDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    fun toEntity(contactInsertDto: ContactInsertDto): ContactEntity

    fun toDomainDto(contactEntity: ContactEntity): ContactDto

    fun toEntity(contactDto: ContactDto): ContactEntity
}
