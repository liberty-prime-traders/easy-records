package app.myliberty.easy_records.platform.business.jurisdiction_type

import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeDto
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface JurisdictionTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: JurisdictionTypeInsertDto): JurisdictionTypeEntity

    fun toDomainDto(jurisdictionTypeEntity: JurisdictionTypeEntity): JurisdictionTypeDto

    fun toEntity(dto: JurisdictionTypeDto): JurisdictionTypeEntity

    fun toResponseDto(dto: JurisdictionTypeDto): JurisdictionTypeResponseDto
}
