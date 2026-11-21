package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type

import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface OrgJurisdictionTaxTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: OrgJurisdictionTaxTypeInsertDto): OrgJurisdictionTaxTypeEntity

    fun toDomainDto(orgJurisdictionTaxTypeEntity: OrgJurisdictionTaxTypeEntity): OrgJurisdictionTaxTypeDto

    fun toEntity(dto: OrgJurisdictionTaxTypeDto): OrgJurisdictionTaxTypeEntity
}
