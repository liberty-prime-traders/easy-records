package app.myliberty.easy_records.organizations.business.core.fund_movement

import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper

@Mapper(config = LibertyMapperConfig::class)
interface FundMovementMapper {

    fun toDto(fundMovementEntity: FundMovementEntity): FundMovementDto

    fun toVoidDto(fundMovementVoidEntity: FundMovementVoidEntity): FundMovementVoidDto
}
