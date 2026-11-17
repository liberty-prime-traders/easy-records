package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper

@Mapper(config = LibertyMapperConfig::class)
interface FiscalPeriodMapper {

    fun toDomainDto(fiscalPeriodEntity: FiscalPeriodEntity): FiscalPeriodDto

    fun toEntity(dto: FiscalPeriodDto): FiscalPeriodEntity
}
