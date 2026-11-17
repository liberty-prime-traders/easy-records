package app.myliberty.easy_records.locations.business.retail.reports.product_sales

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesLineAdjustmentDto
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesLineDto
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesSummaryDto
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.quantity.BaseUnitDisplay
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object ProductSalesReportCalculator {

  private val ONE_HUNDRED = BigDecimal(100)

  fun signedAmount(direction: AdjustmentDirection, calculatedAmount: BigDecimal): BigDecimal = when (direction) {
    AdjustmentDirection.DISCOUNT -> calculatedAmount.negate()
    AdjustmentDirection.SURCHARGE -> calculatedAmount
    AdjustmentDirection.BOTH -> error("A sale adjustment is always a DISCOUNT or a SURCHARGE, never BOTH")
  }

  fun toLineDto(
    rawRow: ProductSalesLineRawRow,
    adjustments: List<ProductSalesLineAdjustmentDto>,
    quantityEntered: String,
    storageUnitsPerBaseUnit: Long
  ): ProductSalesLineDto {
    val grossSales = Decimals.Scale4.round(rawRow.lineTotal)
    val netSales = Decimals.Scale4.round(grossSales + adjustments.sumOf { it.amount })
    val cogs = Decimals.Scale4.round(rawRow.cogsTotal ?: BigDecimal.ZERO)
    val grossProfit = netSales - cogs

    return ProductSalesLineDto(
      id = rawRow.saleLineId,
      saleReferenceNumber = rawRow.saleReferenceNumber,
      dateSold = rawRow.dateSold,
      quantityEntered = quantityEntered,
      baseQuantity = BaseUnitDisplay.baseQuantity(
        rawRow.userProvidedQuantity,
        rawRow.storageUnitsPerUserProvidedUnit,
        storageUnitsPerBaseUnit,
        RoundingMode.HALF_UP
      ),
      grossSales = grossSales,
      netSales = netSales,
      cogs = cogs,
      grossProfit = grossProfit,
      marginPercent = percentOf(grossProfit, netSales),
      adjustments = adjustments
    )
  }

  fun sumByProduct(rawRows: Iterable<ProductSalesSummaryRawRow>): List<ProductSalesSummaryRawRow> {
    val totalsByLocationProductId = HashMap<UUID, ProductSalesSummaryRawRow>()
    rawRows.forEach { totalsByLocationProductId.merge(it.locationProductId, it) { total, row -> total + row } }
    return totalsByLocationProductId.values.toList()
  }

  fun emptySummaryRawRow(locationProductId: UUID): ProductSalesSummaryRawRow = ProductSalesSummaryRawRow(
    locationProductId = locationProductId,
    lineCount = 0,
    storageQuantity = BigDecimal.ZERO,
    grossSales = BigDecimal.ZERO,
    cogsTotal = BigDecimal.ZERO,
    discountMagnitude = BigDecimal.ZERO,
    surchargeMagnitude = BigDecimal.ZERO
  )

  fun toSummaryDto(
    rawRow: ProductSalesSummaryRawRow,
    locationProductSummaryDto: LocationProductSummaryDto,
    quantityBreakdown: QuantityBreakdown,
    baseUnitName: String,
    storageUnitsPerBaseUnit: Long
  ): ProductSalesSummaryDto {
    val baseQuantity = BaseUnitDisplay.storageQuantityInBaseUnit(rawRow.storageQuantity, storageUnitsPerBaseUnit, RoundingMode.HALF_UP)
    val grossSales = Decimals.Scale4.round(rawRow.grossSales)
    val discountTotal = Decimals.Scale4.round(rawRow.discountMagnitude.negate())
    val surchargeTotal = Decimals.Scale4.round(rawRow.surchargeMagnitude)
    val netSales = grossSales + discountTotal + surchargeTotal
    val cogs = Decimals.Scale4.round(rawRow.cogsTotal)
    val grossProfit = netSales - cogs

    return ProductSalesSummaryDto(
      locationProductId = rawRow.locationProductId,
      orgProductId = locationProductSummaryDto.orgProductId,
      productName = locationProductSummaryDto.productName,
      productGroupName = locationProductSummaryDto.productGroupName,
      baseUnitName = baseUnitName,
      lineCount = rawRow.lineCount,
      baseQuantity = baseQuantity,
      baseQuantityText = quantityBreakdown.renderUserProvided(baseQuantity, locationProductSummaryDto.baseUnitId),
      grossSales = grossSales,
      discountTotal = discountTotal,
      surchargeTotal = surchargeTotal,
      netSales = netSales,
      cogs = cogs,
      grossProfit = grossProfit,
      marginPercent = percentOf(grossProfit, netSales),
      marginBeforeAdjustmentsPercent = percentOf(grossSales - cogs, grossSales),
      adjustmentsPercent = percentOf(grossSales - netSales, grossSales),
      avgListPrice = BaseUnitDisplay.pricePerBaseUnit(grossSales, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      avgSellingPrice = BaseUnitDisplay.pricePerBaseUnit(netSales, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      avgCost = BaseUnitDisplay.pricePerBaseUnit(cogs, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      profitPerBaseUnit = BaseUnitDisplay.pricePerBaseUnit(grossProfit, rawRow.storageQuantity, storageUnitsPerBaseUnit)
    )
  }

  fun percentOf(numerator: BigDecimal, denominator: BigDecimal): BigDecimal? =
    if (denominator.signum() == 0) null else Decimals.PercentDisplayScale.divide(numerator.multiply(ONE_HUNDRED), denominator)
}
