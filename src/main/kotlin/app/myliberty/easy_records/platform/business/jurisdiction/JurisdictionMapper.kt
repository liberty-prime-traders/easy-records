package app.myliberty.easy_records.platform.business.jurisdiction

import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionDto
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionInsertDto
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionResponseDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [JurisdictionQualifier::class])
interface JurisdictionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: JurisdictionInsertDto): JurisdictionEntity

    fun toDomainDto(jurisdictionEntity: JurisdictionEntity): JurisdictionDto

    fun toEntity(dto: JurisdictionDto): JurisdictionEntity

    @Mapping(source = "jurisdictionTypeId", target = "jurisdictionType", qualifiedBy = [JurisdictionTypeName::class])
    @Mapping(source = "parentJurisdictionId", target = "parentJurisdiction", qualifiedBy = [ParentJurisdictionName::class])
    @Mapping(source = "id", target = "taxTypes", qualifiedBy = [JurisdictionTaxTypes::class])
    fun toResponseDto(dto: JurisdictionDto, @Context ctx: JurisdictionMappingContext): JurisdictionResponseDto
}
