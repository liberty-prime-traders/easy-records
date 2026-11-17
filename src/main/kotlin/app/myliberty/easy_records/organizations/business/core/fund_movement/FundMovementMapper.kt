package app.myliberty.easy_records.organizations.business.fund_movement

import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FundMovementMapper {

    fun toDto(fundMovementEntity: FundMovementEntity): FundMovementDto

    fun toVoidDto(fundMovementVoidEntity: FundMovementVoidEntity): FundMovementVoidDto
}
