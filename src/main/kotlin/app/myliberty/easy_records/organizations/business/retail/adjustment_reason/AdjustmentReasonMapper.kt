package app.myliberty.easy_records.organizations.business.retail.adjustment_reason

import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonInsertDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
abstract class AdjustmentReasonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    abstract fun toEntity(insertDto: AdjustmentReasonInsertDto): AdjustmentReasonEntity

    abstract fun toDomainDto(adjustmentReasonEntity: AdjustmentReasonEntity): AdjustmentReasonDto

    abstract fun toEntity(dto: AdjustmentReasonDto): AdjustmentReasonEntity
}
