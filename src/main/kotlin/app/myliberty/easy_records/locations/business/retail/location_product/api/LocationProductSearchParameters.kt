package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.util.queries.HasSearchStrategy
import app.myliberty.easy_records.util.queries.SearchStrategy
import java.util.UUID

data class LocationProductSearchParameters(
    val searchText: String? = null,
    val excludeIds: Set<UUID> = emptySet(),
    override val searchStrategy: SearchStrategy = SearchStrategy.NONE
) : HasSearchStrategy<LocationProductSearchParameters> {

    override fun copySelf(searchStrategy: SearchStrategy): LocationProductSearchParameters =
        copy(searchStrategy = searchStrategy)
}
