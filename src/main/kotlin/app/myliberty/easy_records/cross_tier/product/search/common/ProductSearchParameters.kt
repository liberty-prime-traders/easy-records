package app.myliberty.easy_records.cross_tier.product.search.common

import app.myliberty.easy_records.organizations.business.product.api.ProductStatus
import app.myliberty.easy_records.util.queries.HasSearchStrategy
import app.myliberty.easy_records.util.queries.SearchStrategy
import java.util.UUID

data class ProductSearchParameters(
    val searchText: String? = null,
    val referenceNumber: String? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val tagIds: Set<UUID> = emptySet(),
    val excludeIds: Set<UUID> = emptySet(),
    val statusList: Set<ProductStatus> = setOf(ProductStatus.ACTIVE),
    val requireDefaultSalePrice: Boolean = false,
    override val searchStrategy: SearchStrategy = SearchStrategy.NONE
): HasSearchStrategy<ProductSearchParameters> {

    init {
        require(statusList.isNotEmpty()) {
          "statusList cannot be empty. It must contain at least one status value."
        }
    }

    fun extractStatusCodes(): Set<String> {
        return statusList.map { it.code }.toSet()
    }

    override fun copySelf(searchStrategy: SearchStrategy): ProductSearchParameters {
        return copy(searchStrategy = searchStrategy)
    }
}
