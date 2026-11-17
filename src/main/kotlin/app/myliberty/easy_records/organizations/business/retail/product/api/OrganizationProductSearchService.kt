package app.myliberty.easy_records.organizations.business.retail.product.api

import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchService
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductQueryBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OrganizationProductSearchService(
  private val organizationProductFetcher: OrganizationProductFetcher,
  @Value("\${rts.product-search.client-side-filter-threshold}") clientSideFilterThreshold: Int
)
  :
  ProductSearchService<OrganizationProductResponseDto>(
    organizationProductFetcher,
    OrganizationProductQueryBuilder::buildSearchQuery,
    clientSideFilterThreshold
  ) {

  override fun countAllProducts(): Long = organizationProductFetcher.countAllProducts()

  override fun findAllProducts(parameters: ProductSearchParameters): List<OrganizationProductResponseDto> =
    organizationProductFetcher.findAllProducts().filter { it.status in parameters.statusList }
}
