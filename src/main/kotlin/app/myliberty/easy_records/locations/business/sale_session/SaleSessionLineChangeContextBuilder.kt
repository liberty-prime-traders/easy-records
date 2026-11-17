package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductUnitRequestDto
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLineAddDto
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLineUpdateDto
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionLineChangeContext(
    val locationProductSummariesById: Map<UUID, LocationProductSummaryDto>,
    val defaultSalePricesByLocationProductId: Map<UUID, BigDecimal?>,
    val newStorageUnitsPerUserProvidedUnitByLineKey: Map<UUID, Long>,
    val storageUnitsPerBaseUnitByBaseUnitId: Map<UUID, Long>,
)

@Service
class SaleSessionLineChangeContextBuilder(
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    fun buildContext(
        additions: List<SaleSessionLineAddDto>,
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLinesByKey: Map<UUID, SaleSessionLine>,
    ): SaleSessionLineChangeContext {

        val unitChangingLines = updates
            .map { lineUpdateDto -> lineUpdateDto to saleSessionLinesByKey.getValue(lineUpdateDto.identity.key()) }
            .filter { (lineUpdateDto, saleSessionLine) -> saleSessionLine.unitId != lineUpdateDto.unitId }

        val additionLocationProductIds = additions.map { it.locationProductId }.distinct()
        if (additionLocationProductIds.isNotEmpty()) {
            locationProductService.guardAllActive(additionLocationProductIds)
        }

        val locationProductSummariesById = additionLocationProductIds
            .takeIf { it.isNotEmpty() }
            ?.let { locationProductDataFetcher.findSummaryByIds(it) }
            ?: emptyMap()

        val storageUnitsPerUserProvidedUnitByLocationProductId = locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(
            unitChangingLines.map { (lineUpdateDto, saleSessionLine) ->
                LocationProductUnitRequestDto(saleSessionLine.locationProductId, lineUpdateDto.unitId)
            }
        )
        val newStorageUnitsPerUserProvidedUnitByLineKey = unitChangingLines.associate { (lineUpdateDto, saleSessionLine) ->
            lineUpdateDto.identity.key() to storageUnitsPerUserProvidedUnitByLocationProductId.getValue(saleSessionLine.locationProductId)
        }

        val defaultSalePricesByLocationProductId = additionLocationProductIds
            .takeIf { it.isNotEmpty() }
            ?.let { locationProductDataFetcher.getDefaultSalePrices(it) }
            ?: emptyMap()

        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val baseUnitIds = locationProductSummariesById.values.map { it.baseUnitId } +
            updates.map { saleSessionLinesByKey.getValue(it.identity.key()).baseUnitId }
        val storageUnitsPerBaseUnitByBaseUnitId = baseUnitIds.distinct().associateWith { unitConversionGraph.getSizeInRoot(it) }

        return SaleSessionLineChangeContext(
            locationProductSummariesById = locationProductSummariesById,
            defaultSalePricesByLocationProductId = defaultSalePricesByLocationProductId,
            newStorageUnitsPerUserProvidedUnitByLineKey = newStorageUnitsPerUserProvidedUnitByLineKey,
            storageUnitsPerBaseUnitByBaseUnitId = storageUnitsPerBaseUnitByBaseUnitId,
        )
    }
}
