package app.myliberty.easy_records.organizations.business.retail.adjustment_reason

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ADJUSTMENT_REASON])
class AdjustmentReasonCache(
    private val repository: AdjustmentReasonRepository,
    private val mapper: AdjustmentReasonMapper
) {

    @Cacheable
    fun getAll(): Collection<AdjustmentReasonDto> =
        repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: AdjustmentReasonInsertDto): AdjustmentReasonDto {
        val saved = repository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<AdjustmentReasonEntity>) {
        repository.saveAll(entities)
    }
}
