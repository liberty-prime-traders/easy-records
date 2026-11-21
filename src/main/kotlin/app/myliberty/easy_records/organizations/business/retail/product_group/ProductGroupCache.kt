package app.myliberty.easy_records.organizations.business.retail.product_group

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PRODUCT_GROUP])
class ProductGroupCache(
    private val productGroupRepository: ProductGroupRepository,
    private val productGroupMapper: ProductGroupMapper
) {

    @Cacheable
    fun findAllProductGroups(): Collection<ProductGroupDto> =
        productGroupRepository.findAll().map { productGroupMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: ProductGroupInsertDto): ProductGroupDto {
        val saved = productGroupRepository.save(productGroupMapper.toEntity(insertDto))
        return productGroupMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<ProductGroupEntity>): List<ProductGroupEntity> {
        return productGroupRepository.saveAll(entities)
    }

    @CacheEvict(allEntries = true)
    fun save(productGroupDto: ProductGroupDto): ProductGroupDto {
        val saved = productGroupRepository.save(productGroupMapper.toEntity(productGroupDto))
        return productGroupMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteProductGroupById(productGroupId: UUID) {
        productGroupRepository.deleteById(productGroupId)
    }
}
