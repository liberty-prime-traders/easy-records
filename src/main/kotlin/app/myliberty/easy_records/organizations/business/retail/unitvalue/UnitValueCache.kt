package app.myliberty.easy_records.organizations.business.retail.unitvalue

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.QueriedByEmptyIdException
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.UNIT_VALUE])
class UnitValueCache(
    private val unitValueRepository: UnitValueRepository,
    private val unitValueMapper: UnitValueMapper
) {

    @Cacheable
    fun getByUnitGroupId(unitGroupId: UUID?): Collection<UnitValueDto> {
        if (unitGroupId == null) {
            throw QueriedByEmptyIdException()
        }
        return unitValueRepository.findByUnitGroupId(unitGroupId).map { unitValueMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getAllUnitValues(): Collection<UnitValueDto> {
        return unitValueRepository.findAll().map { unitValueMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getUnitNamesById(): Map<UUID, String> =
        unitValueRepository.findAll()
            .mapNotNull { entity -> entity.id?.let { it to entity.name } }
            .toMap()

    @CacheEvict(allEntries = true)
    fun create(insertDto: UnitValueInsertDto): UnitValueDto {
        val saved = unitValueRepository.save(unitValueMapper.toEntity(insertDto))
        return unitValueMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(unitValueEntity: UnitValueEntity): UnitValueDto {
        val saved = unitValueRepository.saveAndFlush(unitValueEntity)
        return unitValueMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(unitValueDto: UnitValueDto): UnitValueDto {
        val saved = unitValueRepository.save(unitValueMapper.toEntity(unitValueDto))
        return unitValueMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<UnitValueEntity>): List<UnitValueEntity> {
        return unitValueRepository.saveAll(entities)
    }

    @CacheEvict(allEntries = true)
    fun deleteAllByIds(ids: Collection<UUID>) {
        unitValueRepository.deleteAllById(ids)
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitValue(id: UUID?) {
        if (id != null) {
            unitValueRepository.deleteById(id)
        }
    }
}
