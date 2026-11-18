package app.myliberty.easy_records.organizations.business.core.location

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION])
class LocationCache(
    private val locationRepository: LocationRepository,
    private val locationMapper: LocationMapper
) {

    @Cacheable
    fun getAllLocations(): Collection<LocationDto> {
        return locationRepository.findAll().map { locationMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: LocationInsertDto, schemaName: String): LocationDto {
        val entity = locationMapper.toEntity(insertDto).apply { this.schemaName = schemaName }
        val saved = locationRepository.save(entity)
        return locationMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(locationDto: LocationDto): LocationDto {
        val saved = locationRepository.save(locationMapper.toEntity(locationDto))
        return locationMapper.toDomainDto(saved)
    }
}
