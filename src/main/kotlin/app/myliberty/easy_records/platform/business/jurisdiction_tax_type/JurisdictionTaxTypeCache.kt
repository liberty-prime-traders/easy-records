package app.myliberty.easy_records.platform.business.jurisdiction_tax_type

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeDto
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TAX_TYPE])
class JurisdictionTaxTypeCache(
    private val jurisdictionTaxTypeRepository: JurisdictionTaxTypeRepository,
    private val mapper: JurisdictionTaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTaxTypeDto> = jurisdictionTaxTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @Cacheable
    fun getActive(): Collection<JurisdictionTaxTypeDto> = jurisdictionTaxTypeRepository.findAll().filter { it.active }.map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: Collection<JurisdictionTaxTypeInsertDto>): List<JurisdictionTaxTypeDto> {
        val saved = jurisdictionTaxTypeRepository.saveAllAndFlush(insertDtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<JurisdictionTaxTypeDto>): List<JurisdictionTaxTypeDto> {
        val saved = jurisdictionTaxTypeRepository.saveAll(dtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }
}
