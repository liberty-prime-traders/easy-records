package app.myliberty.easy_records.organizations.business.product

import app.myliberty.easy_records.organizations.business.product.api.OrganizationProductInsertDto
import app.myliberty.easy_records.organizations.business.product.api.OrganizationProductUpdateDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class OrganizationProductValidator(
    private val organizationProductRepository: OrganizationProductRepository
) {

    fun validateProductUpdate(productUpdateDto: OrganizationProductUpdateDto) {
        val name = StringUtils.getValueOrException(productUpdateDto.productName, NAME_IS_REQUIRED)
        if (productUpdateDto.productGroupId != null && productUpdateDto.productGroupId.isEmpty) {
            throw RtsGenericException("If a product group is provided, it cannot be empty.")
        }

        organizationProductRepository.findFirstByProductNameIgnoreCase(name)?.let {
            if(it.id != productUpdateDto.id) {
                throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name))
            }
        }
    }

    fun validateProductInsert(productInsertDto: OrganizationProductInsertDto) {
        val name = StringUtils.getValueOrException(productInsertDto.productName, NAME_IS_REQUIRED)
        organizationProductRepository.findFirstByProductNameIgnoreCase(name)
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }


    companion object {
        const val NAME_IS_REQUIRED = "A product must have a name"
        const val NAME_ALREADY_EXISTS = "A product with the name %s already exists."
    }
}
