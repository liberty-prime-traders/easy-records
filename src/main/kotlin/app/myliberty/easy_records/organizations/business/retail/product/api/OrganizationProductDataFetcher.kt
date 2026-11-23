package app.myliberty.easy_records.organizations.business.retail.product.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationProductDataFetcher(private val organizationProductRepository: OrganizationProductRepository) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isProductGroupInUse(productGroupId: UUID): Boolean = organizationProductRepository.existsByProductGroupId(productGroupId)
}
