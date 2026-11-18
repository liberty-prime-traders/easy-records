package app.myliberty.easy_records.locations.business.retail.reports.product_sales

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesSummaryDto
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.QuantityBreakdownFactory
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductSalesSummaryAssembler(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val unitConversionGraphFacade: UnitConversionGraphFacade,
  private val unitValueFetcher: UnitValueFetcher,
  private val quantityBreakdownFactory: QuantityBreakdownFactory
) {

  fun assemble(locationProductIds: List<UUID>, rawRows: List<ProductSalesSummaryRawRow>): List<ProductSalesSummaryDto> {
    val summariesById = locationProductDataFetcher.findSummaryByIds(locationProductIds)
    val missingLocationProductIds = locationProductIds.filter { it !in summariesById }
    if (missingLocationProductIds.isNotEmpty()) {
      throw LibertyGenericException("Location products not found: ${missingLocationProductIds.joinToString()}")
    }

    val rawRowsByLocationProductId = rawRows.associateBy { it.locationProductId }
    val unitNamesById = unitValueFetcher.getUnitNamesById()
    val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
    val quantityBreakdownsByBaseUnitId = quantityBreakdownFactory.forBaseUnits(summariesById.values.map { it.baseUnitId })

    return locationProductIds.map { locationProductId ->
      val summary = summariesById.getValue(locationProductId)
      ProductSalesReportCalculator.toSummaryDto(
        rawRow = rawRowsByLocationProductId[locationProductId]
          ?: ProductSalesReportCalculator.emptySummaryRawRow(locationProductId),
        locationProductSummaryDto = summary,
        quantityBreakdown = quantityBreakdownsByBaseUnitId.getValue(summary.baseUnitId),
        baseUnitName = unitNamesById.getValue(summary.baseUnitId),
        storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(summary.baseUnitId)
      )
    }
  }
}
