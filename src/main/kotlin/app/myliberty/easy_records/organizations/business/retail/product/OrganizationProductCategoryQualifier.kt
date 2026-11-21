package app.myliberty.easy_records.organizations.business.retail.product

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupService
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProductCategoryName

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProductCategoryId

@Component
@TransactionalOnOrganizationSchema
class OrganizationProductCategoryQualifier(
  private val productGroupService: ProductGroupService,
  private val productCategoryService: ProductCategoryService
) {

  @ProductCategoryName
  fun resolveCategoryName(productGroupId: UUID?): String? {
    val productGroup = productGroupId?.let { pgId ->
      productGroupService.getAllGroupDtos().find { it.id == pgId }
    }
    val categoryId = productGroup?.categoryId ?: return null
    val categoriesById = productCategoryService.getCategoryNamesById()
    return categoryId.let { categoriesById[it] }
  }

  @ProductCategoryId
  fun resolveCategoryId(productGroupId: UUID?): UUID? {
    val productGroup = productGroupId?.let { pgId ->
      productGroupService.getAllGroupDtos().find { it.id == pgId }
    }
    return productGroup?.categoryId
  }
}
