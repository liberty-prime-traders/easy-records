package app.myliberty.easy_records.platform.business.tax_type

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeDto
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.TAX_TYPE])
class TaxTypeCache(
    private val taxTypeRepository: TaxTypeRepository,
    private val mapper: TaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<TaxTypeDto> = taxTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: TaxTypeInsertDto): TaxTypeDto {
        val saved = taxTypeRepository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: TaxTypeDto): TaxTypeDto {
        val saved = taxTypeRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        taxTypeRepository.deleteById(id)
    }
}
