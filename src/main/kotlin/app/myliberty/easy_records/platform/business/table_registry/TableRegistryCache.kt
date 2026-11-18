package app.myliberty.easy_records.platform.business.table_registry

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.TABLE_REGISTRY])
class TableRegistryCache(
    private val repository: TableRegistryRepository,
    private val mapper: TableRegistryMapper
) {

    @Cacheable
    fun getAllTables(): Collection<TableRegistryDto> = repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun save(dto: TableRegistryDto): TableRegistryDto {
        val saved = repository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<TableRegistryDto>): Collection<TableRegistryDto> {
        val saved = repository.saveAll(dtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }
}
