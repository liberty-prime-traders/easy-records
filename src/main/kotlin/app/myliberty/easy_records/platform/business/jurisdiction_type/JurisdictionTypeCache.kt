package app.myliberty.easy_records.platform.business.jurisdiction_type

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeDto
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TYPE])
class JurisdictionTypeCache(
    private val jurisdictionTypeRepository: JurisdictionTypeRepository,
    private val mapper: JurisdictionTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTypeDto> = jurisdictionTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: JurisdictionTypeInsertDto): JurisdictionTypeDto {
        val saved = jurisdictionTypeRepository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: JurisdictionTypeDto): JurisdictionTypeDto {
        val saved = jurisdictionTypeRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionTypeRepository.deleteById(id)
    }
}
