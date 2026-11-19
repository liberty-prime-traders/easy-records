package app.myliberty.easy_records.platform.business.jurisdiction

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionDto
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION])
class JurisdictionCache(
    private val jurisdictionRepository: JurisdictionRepository,
    private val mapper: JurisdictionMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionDto> = jurisdictionRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: JurisdictionInsertDto): JurisdictionDto {
        val saved = jurisdictionRepository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: JurisdictionDto): JurisdictionDto {
        val saved = jurisdictionRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionRepository.deleteById(id)
    }
}
