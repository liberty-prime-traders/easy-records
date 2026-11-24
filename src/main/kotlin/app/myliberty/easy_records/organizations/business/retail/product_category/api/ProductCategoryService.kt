package app.myliberty.easy_records.organizations.business.retail.product_category.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryCache
import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryMapper
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDataFetcher
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductCategoryService(
    private val productCategoryMapper: ProductCategoryMapper,
    private val productCategoryCache: ProductCategoryCache,
    private val productGroupDataFetcher: ProductGroupDataFetcher,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllCategoryDtos(): Collection<ProductCategoryDto> = productCategoryCache.getAllCategories()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getCategoryNamesById(): Map<UUID, String> = productCategoryCache.getCategoryNamesById()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllCategories(): Collection<ProductCategoryResponseDto> {
        return productCategoryCache.getAllCategories().map { productCategoryMapper.toResponseDto(it) }
    }

    fun createCategory(productCategoryInsertDto: ProductCategoryInsertDto): ProductCategoryResponseDto {
        val categoryName = StringUtils.getValueOrException(productCategoryInsertDto.categoryName, NAME_IS_REQUIRED)
        StringUtils.requireLetterOrDigit(categoryName, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
        productCategoryCache.getAllCategories().find { StringUtils.isEquivalentOnAlphanumerics(it.categoryName, categoryName) }
            ?.let { throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, categoryName))}
        val savedDto = productCategoryCache.create(productCategoryInsertDto)
        return productCategoryMapper.toResponseDto(savedDto)
    }

    fun updateCategory(productCategoryUpdateDto: ProductCategoryUpdateDto): ProductCategoryResponseDto {
        validateCategoryUpdate(productCategoryUpdateDto)
        val existing = productCategoryCache.getAllCategories().find { Objects.equals(productCategoryUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = productCategoryUpdateDto.applyTo(existing)
        val savedDto = productCategoryCache.save(updated)
        return productCategoryMapper.toResponseDto(savedDto)
    }

    private fun validateCategoryUpdate(productCategoryUpdateDto: ProductCategoryUpdateDto) {
        if (productCategoryUpdateDto.categoryName == null) return
        val name = StringUtils.getValueOrException(productCategoryUpdateDto.categoryName, NAME_IS_REQUIRED)
        StringUtils.requireLetterOrDigit(name, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
        productCategoryCache.getAllCategories()
            .find { StringUtils.isEquivalentOnAlphanumerics(it.categoryName, name) && it.id != productCategoryUpdateDto.id }
            ?.let { throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun deleteCategory(id: UUID) {
        if (productGroupDataFetcher.isCategoryInUse(id)) throw LibertyGenericException(CATEGORY_IN_USE)
        productCategoryCache.deleteCategory(id)
    }

    fun bulkCreateValidatedList(insertDtos: List<ProductCategoryInsertDto>): List<ProductCategoryResponseDto> {
        if (insertDtos.isEmpty()) return emptyList()
        val entities = insertDtos.map { productCategoryMapper.toEntity(it) }
        val saved = productCategoryCache.saveAll(entities)
        return saved.map { productCategoryMapper.toDomainDto(it) }.map { productCategoryMapper.toResponseDto(it) }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A category must have a name"
        const val NAME_MUST_CONTAIN_LETTER_OR_DIGIT = "A category name must contain at least one letter or digit"
        const val NAME_ALREADY_EXISTS = "A category with the name %s already exists."
        const val CATEGORY_IN_USE = "A category with product groups cannot be deleted; move or delete those groups first"
    }
}
