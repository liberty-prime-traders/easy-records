package app.myliberty.easy_records.organizations.business.retail.product_group.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupCache
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupMapper
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupRepository
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupValidator
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductGroupService(
    private val productGroupMapper: ProductGroupMapper,
    private val productGroupCache: ProductGroupCache,
    private val productGroupRepository: ProductGroupRepository,
    private val productGroupValidator: ProductGroupValidator,
    private val productCategoryService: ProductCategoryService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllGroupDtos(): Collection<ProductGroupDto> = productGroupCache.findAllProductGroups()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllProductGroups(): Collection<ProductGroupResponseDto> {
        val categoriesById = productCategoryService.getCategoryNamesById()
        return productGroupCache.findAllProductGroups().map {
            productGroupMapper.toResponseDto(it, categoriesById[it.categoryId])
        }
    }

    fun createProductGroup(productGroupInsertDto: ProductGroupInsertDto): ProductGroupResponseDto {
        productGroupValidator.validateProductGroupInsert(productGroupInsertDto)
        val savedDto = productGroupCache.create(productGroupInsertDto)
        val categoriesById = productCategoryService.getCategoryNamesById()
        return productGroupMapper.toResponseDto(savedDto, categoriesById[savedDto.categoryId])
    }

    fun updateProductGroup(productGroupUpdateDto: ProductGroupUpdateDto): ProductGroupResponseDto {
        productGroupRepository.findById(productGroupUpdateDto.id).orElseThrow { UpdatingNonExistingRecordException() }
        productGroupValidator.validateProductGroupUpdate(productGroupUpdateDto)
        val existing = productGroupCache.findAllProductGroups().find { it.id == productGroupUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        val updated = productGroupUpdateDto.applyTo(existing)
        val savedDto = productGroupCache.save(updated)
        val categoriesById = productCategoryService.getCategoryNamesById()
        return productGroupMapper.toResponseDto(savedDto, categoriesById[savedDto.categoryId])
    }

    fun deleteProductGroup(productGroupId: UUID) {
        productGroupCache.deleteProductGroupById(productGroupId)
    }

    fun bulkCreateValidatedList(insertDtos: List<ProductGroupInsertDto>): List<ProductGroupResponseDto> {
        if (insertDtos.isEmpty()) return emptyList()
        val entities = insertDtos.map { productGroupMapper.toEntity(it) }
        val saved = productGroupCache.saveAll(entities)
        val categoriesById = productCategoryService.getCategoryNamesById()
        return saved.map { productGroupMapper.toDomainDto(it) }.map { productGroupMapper.toResponseDto(it, categoriesById[it.categoryId]) }
    }
}
