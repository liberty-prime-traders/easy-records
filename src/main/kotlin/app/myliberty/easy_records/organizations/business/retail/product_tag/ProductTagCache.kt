package app.myliberty.easy_records.organizations.business.product_tag

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_TAG])
class ProductTagCache(
    private val productTagRepository: ProductTagRepository,
    private val productTagMapper: ProductTagMapper
) {

    @Cacheable
    fun findActiveProductTagsByOrgProductId(orgProductId: UUID): Collection<ProductTagDto> =
        productTagRepository.findActiveProductTagsByOrgProductId(orgProductId).map { productTagMapper.toDomainDto(it) }

    @Cacheable
    fun findActiveTagIdsByOrgProductId(orgProductId: UUID): Collection<UUID> =
        productTagRepository.findActiveTagIdsByOrgProductId(orgProductId)

    fun findActiveProductTagsByOrgProductIds(orgProductIds: Collection<UUID>): List<ProductTagDto> {
        if (orgProductIds.isEmpty()) return emptyList()
        return productTagRepository.findActiveProductTagsByOrgProductIds(orgProductIds).map { productTagMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun createProductTags(insertDtos: List<ProductTagInsertDto>): List<ProductTagDto> {
        val entities = insertDtos.map { productTagMapper.toEntity(it) }
        return productTagRepository.saveAllAndFlush(entities).map { productTagMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun saveAllProductTags(productTagDtos: List<ProductTagDto>) {
        productTagRepository.saveAll(productTagDtos.map { productTagMapper.toEntity(it) })
    }
}
