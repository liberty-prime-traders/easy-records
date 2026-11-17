package app.myliberty.easy_records.locations.business.retail.reports.product_purchases

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesDeliveryLineDto
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesReportRowDto
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.quantity.BaseUnitDisplay
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.RoundingMode

object ProductPurchasesReportCalculator {

  fun toRowDto(
    productPurchasesSummaryRawRow: ProductPurchasesSummaryRawRow,
    locationProductSummaryDto: LocationProductSummaryDto,
    categoryName: String,
    quantityBreakdown: QuantityBreakdown,
    storageUnitsPerBaseUnit: Long
  ): ProductPurchasesReportRowDto {
    val averageCostPerBaseUnit = BaseUnitDisplay.pricePerBaseUnit(
      productPurchasesSummaryRawRow.totalCost,
      productPurchasesSummaryRawRow.storageQuantity,
      storageUnitsPerBaseUnit
    )

    return ProductPurchasesReportRowDto(
      locationProductId = productPurchasesSummaryRawRow.locationProductId,
      referenceNumber = locationProductSummaryDto.referenceNumber,
      productName = locationProductSummaryDto.productName,
      productGroupName = locationProductSummaryDto.productGroupName,
      categoryName = categoryName,
      deliveryLineCount = productPurchasesSummaryRawRow.deliveryLineCount,
      receivedQuantityBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(productPurchasesSummaryRawRow.storageQuantity)),
      totalCost = Decimals.MoneyScale.round(productPurchasesSummaryRawRow.totalCost),
      averageCostPerBaseUnit = averageCostPerBaseUnit?.let { DisplayFormatters.formatCurrency(it) }
    )
  }

  fun toDeliveryLineDto(
    productPurchasesDeliveryLineRawRow: ProductPurchasesDeliveryLineRawRow,
    supplierName: String,
    quantityBreakdown: QuantityBreakdown,
    storageUnitsPerBaseUnit: Long
  ): ProductPurchasesDeliveryLineDto = ProductPurchasesDeliveryLineDto(
    id = productPurchasesDeliveryLineRawRow.id,
    purchaseReferenceNumber = productPurchasesDeliveryLineRawRow.purchaseReferenceNumber,
    deliveryReferenceNumber = productPurchasesDeliveryLineRawRow.deliveryReferenceNumber,
    deliveredAt = productPurchasesDeliveryLineRawRow.deliveredAt,
    supplierName = supplierName,
    status = productPurchasesDeliveryLineRawRow.status,
    quantityDeliveredText = quantityBreakdown.renderUserProvided(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.unitId
    ),
    baseQuantity = BaseUnitDisplay.baseQuantity(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.storageUnitsPerUserProvidedUnit,
      storageUnitsPerBaseUnit,
      RoundingMode.HALF_UP
    ),
    unitCost = productPurchasesDeliveryLineRawRow.unitCost,
    lineTotal = Decimals.MoneyScale.multiply(
      productPurchasesDeliveryLineRawRow.userProvidedQuantityDelivered,
      productPurchasesDeliveryLineRawRow.unitCost
    )
  )
}
