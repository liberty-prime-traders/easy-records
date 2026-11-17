package app.myliberty.easy_records.platform.business.tax_type

import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeDto
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeInsertDto
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface TaxTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: TaxTypeInsertDto): TaxTypeEntity

    fun toDomainDto(taxTypeEntity: TaxTypeEntity): TaxTypeDto

    fun toEntity(dto: TaxTypeDto): TaxTypeEntity

    fun toResponseDto(dto: TaxTypeDto): TaxTypeResponseDto
}
