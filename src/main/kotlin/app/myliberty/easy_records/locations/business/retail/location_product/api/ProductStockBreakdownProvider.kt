package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.QuantityBreakdownFactory
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductStockBreakdownProvider(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
) {

    fun forLocationProduct(locationProductId: UUID): QuantityBreakdown {
        return forLocationProducts(listOf(locationProductId)).getValue(locationProductId)
    }

    fun forLocationProducts(locationProductIds: Collection<UUID>): Map<UUID, QuantityBreakdown> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIds)
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(baseUnitIdByLocationProductId.values)
        return baseUnitIdByLocationProductId.mapValues { (_, baseUnitId) -> breakdownByBaseUnitId.getValue(baseUnitId) }
    }
}
