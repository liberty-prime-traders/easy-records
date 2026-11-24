package app.myliberty.easy_records.organizations.business.retail.product_bulk_import.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductInsertDto
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductService
import app.myliberty.easy_records.organizations.business.retail.product_bulk_import.BulkProductImportValidator
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupService
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class BulkProductImportService(
    private val bulkProductImportValidator: BulkProductImportValidator,
    private val productCategoryService: ProductCategoryService,
    private val productGroupService: ProductGroupService,
    private val organizationProductService: OrganizationProductService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun bulkImport(bulkProductImportRequestDto: BulkProductImportRequestDto) {
        val errors = bulkProductImportValidator.validate(bulkProductImportRequestDto)
        if (errors.isNotEmpty()) {
            throw LibertyGenericException("Bulk product import validation failed", errors)
        }
        val categoryIdByNormalizedName = saveOrReuseCategories(bulkProductImportRequestDto.categories)
        val groupIdByNormalizedName = saveOrReuseProductGroups(bulkProductImportRequestDto.productGroups, categoryIdByNormalizedName)
        saveProducts(bulkProductImportRequestDto.products, groupIdByNormalizedName)
    }

    private fun saveOrReuseCategories(categories: List<ProductCategoryBulkInsertDto>): Map<String, UUID> {
        val categoryIdByNormalizedName = productCategoryService.getAllCategoryDtos()
            .associateTo(mutableMapOf()) { StringUtils.alphanumericKey(it.categoryName!!) to it.id }

        val newCategories = categories.filter {
            !categoryIdByNormalizedName.containsKey(StringUtils.alphanumericKey(it.name!!))
        }
        val saved = productCategoryService.bulkCreateValidatedList(
            newCategories.map { ProductCategoryInsertDto(categoryName = it.name, description = it.description) }
        )
        saved.forEach { categoryIdByNormalizedName[StringUtils.alphanumericKey(it.categoryName!!)] = it.id }

        return categoryIdByNormalizedName
    }

    private fun saveOrReuseProductGroups(
        productGroups: List<ProductGroupBulkInsertDto>,
        categoryIdByNormalizedName: Map<String, UUID>
    ): Map<String, UUID> {
        val groupIdByNormalizedName = productGroupService.getAllGroupDtos()
            .associateTo(mutableMapOf()) { StringUtils.alphanumericKey(it.groupName) to it.id }

        val newProductGroups = productGroups.filter {
            !groupIdByNormalizedName.containsKey(StringUtils.alphanumericKey(it.name!!))
        }
        val saved = productGroupService.bulkCreateValidatedList(
            newProductGroups.map {
                ProductGroupInsertDto(
                    groupName = it.name!!,
                    description = it.description,
                    categoryId = categoryIdByNormalizedName.getValue(StringUtils.alphanumericKey(it.categoryName!!))
                )
            }
        )
        saved.forEach { groupIdByNormalizedName[StringUtils.alphanumericKey(it.groupName!!)] = it.id }

        return groupIdByNormalizedName
    }

    private fun saveProducts(products: List<ProductBulkInsertDto>, groupIdByNormalizedName: Map<String, UUID>) {
        val unitIdByNormalizedCode = unitValueFetcher.getAllUnitValues()
            .associate { StringUtils.normalizeForComparison(it.code) to it.id }

        val insertDtos = products.map { product ->
            OrganizationProductInsertDto(
                productName = product.name!!,
                description = product.description,
                productGroupId = groupIdByNormalizedName.getValue(StringUtils.alphanumericKey(product.groupName!!)),
                baseUnitId = unitIdByNormalizedCode.getValue(StringUtils.normalizeForComparison(product.unitCode!!))
            )
        }
        organizationProductService.bulkCreateValidatedList(insertDtos)
    }
}
