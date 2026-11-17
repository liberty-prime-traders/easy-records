package app.myliberty.easy_records.platform.business.feature

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.platform.business.feature.api.FeatureDto
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.FEATURE])
class FeatureCache(
    private val featureRepository: FeatureRepository,
    private val featureMapper: FeatureMapper
) {

    @Cacheable
    fun getAll(): Collection<FeatureDto> = featureRepository.findAll().map {
        featureMapper.toDomainDto(it)
    }

    @CacheEvict(allEntries = true)
    fun update(updated: FeatureDto) {
        featureRepository.save(featureMapper.toEntity(updated))
    }
}
