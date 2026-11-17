package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductResponseDto
import app.myliberty.easy_records.locations.business.opening_stock.api.OpeningStockAmountsFetcher
import app.myliberty.easy_records.locations.business.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.organizations.business.unitconversion.api.QuantityBreakdownFactory
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.quantity.BaseUnitDisplay
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.RoundingMode

@Component
class LocationProductEnricher(
    private val locationProductMapper: LocationProductMapper,
    private val unitValueFetcher: UnitValueFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val openingStockAmountsFetcher: OpeningStockAmountsFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    fun convertToResponseDto(dtos: List<LocationProductDto>): List<LocationProductResponseDto> {
        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val productIds = dtos.map { it.id }
        val balances = stockBalanceFetcher.getLatestBalances(productIds)
        val openingStockAmounts = openingStockAmountsFetcher.getAmountsByProductIds(productIds)
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(dtos.map { it.baseUnitId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return dtos.map {
            val openingStock = openingStockAmounts[it.id]
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(it.baseUnitId)
            val quantityBreakdown = breakdownByBaseUnitId.getValue(it.baseUnitId)
            val openingStockStorageQuantity = openingStock?.let { amounts ->
                ExactQuantity.ofStorageFromUserProvided(amounts.userProvidedQuantity, amounts.storageUnitsPerUserProvidedUnit)
            }
            locationProductMapper.toResponseDto(
                it,
                LocationProductContext(
                    unitName = unitNamesById.getValue(it.baseUnitId),
                    balance = BaseUnitDisplay.storageQuantityInBaseUnit(balances.getValue(it.id), storageUnitsPerBaseUnit, RoundingMode.DOWN),
                    openingStockQuantity = openingStockStorageQuantity?.toDisplayQuantity(storageUnitsPerBaseUnit, RoundingMode.DOWN),
                    openingStockUnitCost = openingStock?.unitCost,
                    balanceBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(balances.getValue(it.id))),
                    openingStockBreakdown = openingStockStorageQuantity?.let { quantity -> quantityBreakdown.render(quantity) }
                )
            )
        }
    }
}
