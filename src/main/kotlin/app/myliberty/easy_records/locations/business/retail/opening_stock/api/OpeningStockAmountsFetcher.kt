package app.myliberty.easy_records.locations.business.retail.opening_stock.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.opening_stock.OpeningStockRepository
import org.springframework.stereotype.Component
import java.util.UUID


@Component
@TransactionalOnLocationSchema(readOnly = true)
class OpeningStockAmountsFetcher(
    private val openingStockRepository: OpeningStockRepository
) {

    fun getAmountsByProductIds(locationProductIds: Collection<UUID>): Map<UUID, OpeningStockAmountsDto> {
        if (locationProductIds.isEmpty()) return emptyMap()
        return openingStockRepository.findByLocationProductIdIn(locationProductIds)
            .associate { it.locationProductId to OpeningStockAmountsDto(
                userProvidedQuantity = it.userProvidedQuantity,
                unitId = it.unitId,
                storageUnitsPerUserProvidedUnit = it.storageUnitsPerUserProvidedUnit,
                unitCost = it.unitCost()
            ) }
    }
}
