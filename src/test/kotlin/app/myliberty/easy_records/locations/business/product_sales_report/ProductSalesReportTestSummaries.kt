package app.myliberty.easy_records.locations.business.product_sales_report

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.product_sales_report.api.ProductSalesSummaryDto
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

object ProductSalesReportTestSummaries {

  // Built through the real calculator so a test row can never carry figures the endpoints would not produce.
  fun summary(
    productName: String,
    grossSales: String,
    cogs: String,
    discount: String = "0",
    surcharge: String = "0",
    productGroupName: String = "Group",
    locationProductId: UUID = UUID.randomUUID()
  ): ProductSalesSummaryDto {
    val baseUnitId = UUID.randomUUID()
    return ProductSalesReportCalculator.toSummaryDto(
      rawRow = rawRow(locationProductId, grossSales, cogs, discount, surcharge),
      locationProductSummaryDto = LocationProductSummaryDto(
        locationProductId, "LP000001", UUID.randomUUID(), productName, productGroupName, baseUnitId
      ),
      quantityBreakdown = pieceBreakdown(baseUnitId),
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )
  }

  fun pieceBreakdown(baseUnitId: UUID): QuantityBreakdown = QuantityBreakdown.forBaseUnit(
    listOf(BreakdownUnit(baseUnitId, "Piece", 1, baseUnitId, OffsetDateTime.now())), baseUnitId
  )

  fun rawRow(locationProductId: UUID, grossSales: String, cogs: String, discount: String = "0", surcharge: String = "0") =
    ProductSalesSummaryRawRow(
      locationProductId = locationProductId,
      lineCount = 1,
      storageQuantity = BigDecimal(10),
      grossSales = BigDecimal(grossSales),
      cogsTotal = BigDecimal(cogs),
      discountMagnitude = BigDecimal(discount),
      surchargeMagnitude = BigDecimal(surcharge)
    )
}
