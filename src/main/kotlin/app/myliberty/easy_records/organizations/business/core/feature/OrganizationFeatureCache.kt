package app.myliberty.easy_records.organizations.business.core.feature

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_FEATURE])
class OrganizationFeatureCache(
    private val repository: OrganizationFeatureRepository,
    private val mapper: OrganizationFeatureMapper
) {

    @Cacheable
    fun getAll(): List<OrganizationFeatureDto> = repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: List<OrganizationFeatureDto>): List<OrganizationFeatureDto> {
        val saved = repository.saveAllAndFlush(dtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }
}
