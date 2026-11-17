package app.myliberty.easy_records.platform.business.feature

import app.myliberty.easy_records.platform.business.feature.api.FeatureDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FeatureMapper {
    fun toDomainDto(featureEntity: FeatureEntity): FeatureDto

    fun toEntity(dto: FeatureDto): FeatureEntity
}
