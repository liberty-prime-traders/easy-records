package app.myliberty.easy_records.locations.business.retail.reports.product_purchases

import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesDeliveryLinesParameters
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesReportParameters
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import app.myliberty.easy_records.util.queries.QueryParameterNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class ProductPurchasesReportQueryBuilderTest {

  private val range = ProductPurchasesDeliveredRange(
    OffsetDateTime.parse("2026-10-01T00:00:00+00:00"),
    OffsetDateTime.parse("2026-10-08T00:00:00+00:00")
  )
  private val locationProductId = UUID.randomUUID()

  @Test
  fun `summary counts only applied deliveries and active lines`() {
    val context = ProductPurchasesReportQueryBuilder.buildPredicate(
      ProductPurchasesReportParameters(range.fromInclusive, range.beforeExclusive),
      range
    )

    assertTrue(context.whereClauses.contains("pd.status = ANY(:appliedDeliveryStatuses)"))
    assertTrue(context.whereClauses.contains("pdl.status = :activeLineStatus"))
    assertEquals(listOf("R"), (context.params["appliedDeliveryStatuses"] as Array<*>).toList())
    assertEquals("ACT", context.params["activeLineStatus"])
  }

  @Test
  fun `expansion applies the same header filter but not the active line filter`() {
    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      ProductPurchasesDeliveryLinesParameters(range.fromInclusive, range.beforeExclusive),
      range,
      null
    )

    assertTrue(sqlQuery.sql.contains("pd.status = ANY(:appliedDeliveryStatuses)"))
    assertFalse(sqlQuery.sql.contains("activeLineStatus"))
    assertFalse(sqlQuery.params.containsKey(QueryParameterNames.CURSOR_ID))
  }

  @Test
  fun `expansion adds a keyset condition when a cursor is given`() {
    val cursor = KeysetSearchCursor(range.beforeExclusive, UUID.randomUUID())

    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      ProductPurchasesDeliveryLinesParameters(range.fromInclusive, range.beforeExclusive),
      range,
      cursor
    )

    assertTrue(sqlQuery.sql.contains("(pd.delivered_at, pdl.id) <"))
    assertEquals(cursor.id, sqlQuery.params[QueryParameterNames.CURSOR_ID])
  }

  @Test
  fun `summary filters bind the delivery window, the products, the categories and the suppliers it was given`() {
    val categoryId = UUID.randomUUID()
    val supplierId = UUID.randomUUID()

    val context = ProductPurchasesReportQueryBuilder.buildPredicate(
      ProductPurchasesReportParameters(
        range.fromInclusive,
        range.beforeExclusive,
        locationProductIds = listOf(locationProductId),
        categoryIds = listOf(categoryId),
        supplierIds = listOf(supplierId)
      ),
      range
    )

    assertTrue(context.whereClauses.contains("pd.delivered_at >= :deliveredFrom"))
    assertTrue(context.whereClauses.contains("pd.delivered_at < :deliveredBefore"))
    assertEquals(range.fromInclusive, context.params["deliveredFrom"])
    assertEquals(range.beforeExclusive, context.params["deliveredBefore"])
    assertTrue(context.whereClauses.contains("pl.location_product_id = ANY(:locationProductIds)"))
    assertEquals(listOf(locationProductId), (context.params["locationProductIds"] as Array<*>).toList())
    assertTrue(context.whereClauses.contains("lp.category_id = ANY(:categoryIds)"))
    assertEquals(listOf(categoryId), (context.params["categoryIds"] as Array<*>).toList())
    assertTrue(context.whereClauses.contains("p.supplier_id = ANY(:supplierIds)"))
    assertEquals(listOf(supplierId), (context.params["supplierIds"] as Array<*>).toList())
  }

  @Test
  fun `summary without optional filters adds no product, category or supplier condition`() {
    val context = ProductPurchasesReportQueryBuilder.buildPredicate(
      ProductPurchasesReportParameters(range.fromInclusive, range.beforeExclusive),
      range
    )

    assertFalse(context.params.containsKey("locationProductIds"))
    assertFalse(context.params.containsKey("categoryIds"))
    assertFalse(context.params.containsKey("supplierIds"))
  }

  @Test
  fun `expansion binds the product, the supplier filter and the delivery window`() {
    val supplierId = UUID.randomUUID()

    val sqlQuery = ProductPurchasesReportQueryBuilder.buildDeliveryLinesQuery(
      locationProductId,
      ProductPurchasesDeliveryLinesParameters(range.fromInclusive, range.beforeExclusive, listOf(supplierId)),
      range,
      null
    )

    assertTrue(sqlQuery.sql.contains("pl.location_product_id = :locationProductId"))
    assertEquals(locationProductId, sqlQuery.params["locationProductId"])
    assertTrue(sqlQuery.sql.contains("p.supplier_id = ANY(:supplierIds)"))
    assertEquals(listOf(supplierId), (sqlQuery.params["supplierIds"] as Array<*>).toList())
    assertEquals(range.fromInclusive, sqlQuery.params["deliveredFrom"])
    assertEquals(range.beforeExclusive, sqlQuery.params["deliveredBefore"])
  }
}
