package app.myliberty.easy_records.organizations.business.retail.product_group.api

import app.myliberty.easy_records.organizations.business.retail.product_group.ProductGroupCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductGroupDataFetcher(private val productGroupCache: ProductGroupCache) {

    fun isCategoryInUse(categoryId: UUID): Boolean = productGroupCache.findAllProductGroups().any { it.categoryId == categoryId }
}
