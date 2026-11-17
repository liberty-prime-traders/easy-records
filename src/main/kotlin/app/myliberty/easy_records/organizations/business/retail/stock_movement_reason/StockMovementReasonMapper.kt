package app.myliberty.easy_records.organizations.business.stock_movement_reason

import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.StockMovementReasonInsertDto
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.StockMovementReasonResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
abstract class StockMovementReasonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    abstract fun toEntity(insertDto: StockMovementReasonInsertDto): StockMovementReasonEntity

    abstract fun toDomainDto(stockMovementReasonEntity: StockMovementReasonEntity): StockMovementReasonDto

    abstract fun toEntity(dto: StockMovementReasonDto): StockMovementReasonEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    abstract fun toResponseDto(dto: StockMovementReasonDto): StockMovementReasonResponseDto
}
