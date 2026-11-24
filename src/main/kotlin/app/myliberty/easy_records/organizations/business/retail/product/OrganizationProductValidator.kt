package app.myliberty.easy_records.organizations.business.retail.product

import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductInsertDto
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductUpdateDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupService
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationProductValidator(
    private val organizationProductRepository: OrganizationProductRepository,
    private val productGroupService: ProductGroupService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun validateProductUpdate(productUpdateDto: OrganizationProductUpdateDto) {
        if (productUpdateDto.productGroupId != null) {
            if (productUpdateDto.productGroupId.isEmpty) {
                throw LibertyGenericException(PRODUCT_GROUP_CANNOT_BE_EMPTY)
            }
            requireProductGroupExists(productUpdateDto.productGroupId.get())
        }
        if (productUpdateDto.productName == null) return

        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        requireNameIsFree(name, excludedProductId = productUpdateDto.id)
    }

    fun validateProductInsert(productInsertDto: OrganizationProductInsertDto) {
        val name = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        requireNameIsFree(name, excludedProductId = null)
        requireProductGroupExists(productInsertDto.productGroupId)
        if (productInsertDto.baseUnitId !in unitValueFetcher.getUnitNamesById()) {
            throw LibertyGenericException(BASE_UNIT_DOES_NOT_EXIST)
        }
    }

    private fun requireNameIsFree(name: String, excludedProductId: UUID?) {
        StringUtils.requireLetterOrDigit(name, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
        organizationProductRepository.findFirstByNameKey(StringUtils.alphanumericKey(name))
            ?.takeIf { it.id != excludedProductId }
            ?.let { throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    private fun requireProductGroupExists(productGroupId: UUID) {
        if (productGroupService.getAllGroupDtos().none { it.id == productGroupId }) {
            throw LibertyGenericException(PRODUCT_GROUP_DOES_NOT_EXIST)
        }
    }


    companion object {
        const val NAME_IS_REQUIRED = "A product must have a name"
        const val NAME_MUST_CONTAIN_LETTER_OR_DIGIT = "A product name must contain at least one letter or digit"
        const val NAME_ALREADY_EXISTS = "A product with the name %s already exists."
        const val PRODUCT_GROUP_CANNOT_BE_EMPTY = "If a product group is provided, it cannot be empty."
        const val PRODUCT_GROUP_DOES_NOT_EXIST = "The product group ID provided does not exist."
        const val BASE_UNIT_DOES_NOT_EXIST = "The base unit ID provided does not exist."
    }
}
