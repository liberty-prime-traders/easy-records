package app.myliberty.easy_records.organizations.business.retail.product_group

import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupUpdateDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductGroupValidator(
  private val productGroupCache: ProductGroupCache,
  private val productCategoryService: ProductCategoryService
) {

  fun validateProductGroupUpdate(dto: ProductGroupUpdateDto) {
    if (dto.groupName != null) {
      val name = StringUtils.getValueOrException(dto.groupName, NAME_IS_REQUIRED)
      StringUtils.requireLetterOrDigit(name, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
      productGroupCache.findAllProductGroups()
        .find { StringUtils.isEquivalentOnAlphanumerics(it.groupName, name) && it.id != dto.id }?.let {
          throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, name))
      }
    }
    if (dto.categoryId != null && dto.categoryId.isEmpty) {
      throw LibertyGenericException(CATEGORY_CANNOT_BE_EMPTY)
    }
    validateProductGroupCategory(dto.categoryId?.get())
  }

  fun validateProductGroupInsert(productGroupInsertDto: ProductGroupInsertDto) {
    val name = StringUtils.getValueOrException(productGroupInsertDto.groupName, NAME_IS_REQUIRED)
    StringUtils.requireLetterOrDigit(name, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
    productGroupCache.findAllProductGroups()
      .find { StringUtils.isEquivalentOnAlphanumerics(it.groupName, name) }
      ?.let { throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, name))
    }
    validateProductGroupCategory(productGroupInsertDto.categoryId)
  }

  private fun validateProductGroupCategory(categoryId: UUID?) {
    categoryId?.let {
      productCategoryService.getAllCategoryDtos().find { it.id == categoryId }
        ?: throw LibertyGenericException(INVALID_CATEGORY_ID)
    }
  }

  companion object {
    const val NAME_IS_REQUIRED = "A product group must have a name"
    const val NAME_MUST_CONTAIN_LETTER_OR_DIGIT = "A product group name must contain at least one letter or digit"
    const val NAME_ALREADY_EXISTS = "A product group with the name %s already exists."
    const val INVALID_CATEGORY_ID = "The category ID provided does not exist."
    const val CATEGORY_CANNOT_BE_EMPTY = "If a category is provided, it cannot be empty."
  }
}
