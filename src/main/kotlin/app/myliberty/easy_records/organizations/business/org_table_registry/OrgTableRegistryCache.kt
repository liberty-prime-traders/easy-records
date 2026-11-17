package app.myliberty.easy_records.organizations.business.org_table_registry

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.organizations.business.org_table_registry.api.OrgTableRegistryDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.ORG_TABLE_REGISTRY])
class OrgTableRegistryCache(
    private val repository: OrgTableRegistryRepository,
    private val orgTableRegistryMapper: OrgTableRegistryMapper
) {

    @Cacheable
    fun getAllTables(): Collection<OrgTableRegistryDto> =
        repository.findAll().map { orgTableRegistryMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsertTable(dto: OrgTableRegistryDto) {
        repository.save(orgTableRegistryMapper.toEntity(dto))
    }
}
