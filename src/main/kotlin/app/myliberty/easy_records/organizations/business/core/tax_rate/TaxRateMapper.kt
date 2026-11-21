package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface TaxRateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: TaxRateInsertDto): TaxRateEntity

    fun toDomainDto(taxRateEntity: TaxRateEntity): TaxRateDto

    fun toEntity(taxRateDto: TaxRateDto): TaxRateEntity

    @Mapping(target = "taxLabel", ignore = true)
    @Mapping(target = "parentIsActive", ignore = true)
    fun toResponseDtoBase(taxRateDto: TaxRateDto): TaxRateResponseDto

    fun toResponseDto(taxRateDto: TaxRateDto, taxLabel: String?, parentIsActive: Boolean): TaxRateResponseDto =
        toResponseDtoBase(taxRateDto).copy(taxLabel = taxLabel, parentIsActive = parentIsActive)
}
