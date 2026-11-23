package app.myliberty.easy_records.organizations.business.retail.product_group.api

import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductDataFetcher
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupCache
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupMapper
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupRepository
import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupValidator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class ProductGroupServiceDeleteTest {

    private val productGroupCache = mock(ProductGroupCache::class.java)
    private val organizationProductDataFetcher = mock(OrganizationProductDataFetcher::class.java)
    private val service = ProductGroupService(
        mock(ProductGroupMapper::class.java),
        productGroupCache,
        mock(ProductGroupRepository::class.java),
        mock(ProductGroupValidator::class.java),
        mock(ProductCategoryService::class.java),
        organizationProductDataFetcher
    )

    @Test
    fun `a group that products still use cannot be deleted`() {
        val productGroupId = UUID.randomUUID()
        `when`(organizationProductDataFetcher.isProductGroupInUse(productGroupId)).thenReturn(true)

        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteProductGroup(productGroupId) }

        assertEquals(ProductGroupService.GROUP_IN_USE, exception.message)
        verify(productGroupCache, never()).deleteProductGroupById(productGroupId)
    }
}
