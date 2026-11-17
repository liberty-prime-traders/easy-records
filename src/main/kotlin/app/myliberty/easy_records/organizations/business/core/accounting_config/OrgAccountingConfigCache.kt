package app.myliberty.easy_records.organizations.business.core.accounting_config

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_ACCOUNTING_CONFIG])
class OrgAccountingConfigCache(
    private val repository: OrgAccountingConfigRepository,
    private val mapper: OrgAccountingConfigMapper
) {

    @Cacheable
    fun get(): OrgAccountingConfigDto? = repository.findAll()
        .firstOrNull()
        ?.let { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrgAccountingConfigInsertDto): OrgAccountingConfigDto {
        val saved = repository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrgAccountingConfigDto): OrgAccountingConfigDto {
        val saved = repository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }
}
