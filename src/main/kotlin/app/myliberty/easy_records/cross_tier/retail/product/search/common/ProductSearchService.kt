package app.myliberty.easy_records.cross_tier.product.search.common

import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.FetchesUsingSmartTextStrategy
import app.myliberty.easy_records.util.queries.QueryFormatter
import app.myliberty.easy_records.util.queries.QueryParameterNames
import app.myliberty.easy_records.util.queries.SearchStrategyExecutor
import app.myliberty.easy_records.util.queries.SqlQuery

abstract class ProductSearchService<DTO>(
  private val fetcher: FetchesUsingSmartTextStrategy<ProductSearchParameters, DTO>,
  private val queryBuilder: (ProductSearchParameters, String) -> SqlQuery,
  private val clientSideFilterThreshold: Int
) {

  protected abstract fun countAllProducts(): Long
  protected abstract fun findAllProducts(parameters: ProductSearchParameters): List<DTO>

  open fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<DTO, String> {

    if (shouldUseClientSideFiltering()) {
      return loadAllProductsForClientFiltering(pageRequest.parameters)
    }

    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagIds
    )
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return fetcher.fetch(pageRequest, false)
    }

    return SearchStrategyExecutor.executeWithRetry(
      searchText = searchText,
      pageRequest = pageRequest,
      fetcher = fetcher
    )
  }

  private fun shouldUseClientSideFiltering(): Boolean {
    return countAllProducts() <= clientSideFilterThreshold
  }

  private fun loadAllProductsForClientFiltering(parameters: ProductSearchParameters): PageResponse<DTO, String> {
    return PageResponse(
      currentCursor = "",
      hasMore = false,
      contents = findAllProducts(parameters),
      requireClientSideFilter = true
    )
  }

  open fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagIds
    )
    val sqlQuery = queryBuilder(pageRequest.parameters, pageRequest.previousCursor)
    return QueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize, QueryParameterNames.PAGE_SIZE)
  }
}
