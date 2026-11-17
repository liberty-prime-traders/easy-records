package app.myliberty.easy_records.locations.business.product_sales_report

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ProductSalesReportFetcher(
  private val productSalesReportExecutor: ProductSalesReportExecutor
) {

  fun fetchLines(
    locationProductId: UUID,
    dateRange: DateSoldRange,
    cursor: KeysetSearchCursor?,
    requestedSize: Int
  ): List<ProductSalesLineRawRow> {
    val predicate = ProductSalesReportQueryBuilder.buildPredicate(listOf(locationProductId), dateRange)
    val sqlQuery = ProductSalesReportQueryBuilder.buildLinesQuery(predicate, cursor)
    return productSalesReportExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
      .map { ProductSalesReportRowMapper.lineFromTuple(it) }
  }

  fun fetchLineAdjustments(saleLineIds: List<UUID>): List<ProductSalesLineAdjustmentRawRow> {
    if (saleLineIds.isEmpty()) return emptyList()
    val sqlQuery = ProductSalesReportQueryBuilder.buildLineAdjustmentsQuery(saleLineIds)
    return productSalesReportExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { ProductSalesReportRowMapper.adjustmentFromTuple(it) }
  }

  fun fetchSummaries(locationProductIds: List<UUID>, dateRange: DateSoldRange): List<ProductSalesSummaryRawRow> {
    val predicate = ProductSalesReportQueryBuilder.buildPredicate(locationProductIds, dateRange)
    val sqlQuery = ProductSalesReportQueryBuilder.buildSummaryQuery(predicate)
    return productSalesReportExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { ProductSalesReportRowMapper.summaryFromTuple(it) }
  }

  // The result is cached per day, so the load gets a longer timeout than a per-request query.
  fun fetchDailySummaries(window: ProductSalesReportWindow): List<ProductSalesDailyRawRow> {
    val predicate = ProductSalesReportQueryBuilder.buildAllProductsPredicate(window.toDateRange())
    val sqlQuery = ProductSalesReportQueryBuilder.buildDailySummaryQuery(predicate, SessionContextProvider.getOrgTimezone())
    return productSalesReportExecutor.executeUnpaged(sqlQuery, setTimeout = true, queryTimeoutMs = DAILY_SUMMARY_TIMEOUT_MS)
      .map { ProductSalesReportRowMapper.dailySummaryFromTuple(it) }
  }

  private companion object {
    const val DAILY_SUMMARY_TIMEOUT_MS = 30_000
  }
}
