package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_PRODUCT])
class LocationProductCache(
    private val locationProductRepository: LocationProductRepository,
    private val locationProductMapper: LocationProductMapper
) {

    @Cacheable
    fun findAllLocationProducts(): List<LocationProductDto> =
        locationProductRepository.findAllLocationProducts().map { locationProductMapper.toDomainDto(it) }

    @Cacheable
    fun countAllLocationProducts(): Long = locationProductRepository.count()

    @CacheEvict(allEntries = true)
    fun create(insertDto: LocationProductInsertDto): LocationProductDto {
        val saved = locationProductRepository.save(locationProductMapper.toEntity(insertDto))
        return locationProductMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: LocationProductDto): LocationProductDto {
        val saved = locationProductRepository.save(locationProductMapper.toEntity(dto))
        return locationProductMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun evictAll() {}
}
