package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TAX_RATE])
class TaxRateCache(
    private val taxRateRepository: TaxRateRepository,
    private val taxRateMapper: TaxRateMapper
) {

    @Cacheable
    fun getAll(): Collection<TaxRateDto> = taxRateRepository.findAll().map { taxRateMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: TaxRateInsertDto): TaxRateDto {
        val saved = taxRateRepository.save(taxRateMapper.toEntity(insertDto))
        return taxRateMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(taxRateDto: TaxRateDto): TaxRateDto {
        val saved = taxRateRepository.save(taxRateMapper.toEntity(taxRateDto))
        return taxRateMapper.toDomainDto(saved)
    }
}
