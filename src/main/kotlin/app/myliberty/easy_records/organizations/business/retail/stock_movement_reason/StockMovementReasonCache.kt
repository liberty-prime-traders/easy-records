package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.STOCK_MOVEMENT_REASON])
class StockMovementReasonCache(
    private val repository: StockMovementReasonRepository,
    private val mapper: StockMovementReasonMapper
) {

    @Cacheable
    fun getAll(): Collection<StockMovementReasonDto> =
        repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: StockMovementReasonInsertDto): StockMovementReasonDto {
        val saved = repository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: StockMovementReasonDto): StockMovementReasonDto {
        val saved = repository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        repository.deleteById(id)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<StockMovementReasonEntity>) {
        repository.saveAll(entities)
    }
}
