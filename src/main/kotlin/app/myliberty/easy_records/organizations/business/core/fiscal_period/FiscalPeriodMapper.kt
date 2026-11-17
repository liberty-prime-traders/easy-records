package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FiscalPeriodMapper {

    fun toDomainDto(fiscalPeriodEntity: FiscalPeriodEntity): FiscalPeriodDto

    fun toEntity(dto: FiscalPeriodDto): FiscalPeriodEntity
}
