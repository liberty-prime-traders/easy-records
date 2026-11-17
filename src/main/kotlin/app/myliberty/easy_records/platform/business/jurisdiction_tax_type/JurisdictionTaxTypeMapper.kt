package app.myliberty.easy_records.platform.business.jurisdiction_tax_type

import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeDto
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface JurisdictionTaxTypeMapper {

    fun toDomainDto(jurisdictionTaxTypeEntity: JurisdictionTaxTypeEntity): JurisdictionTaxTypeDto

    fun toEntity(dto: JurisdictionTaxTypeDto): JurisdictionTaxTypeEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "active", constant = "true")
    fun toEntity(insertDto: JurisdictionTaxTypeInsertDto): JurisdictionTaxTypeEntity
}
