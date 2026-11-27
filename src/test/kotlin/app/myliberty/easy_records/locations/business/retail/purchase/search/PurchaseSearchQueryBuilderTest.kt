package app.myliberty.easy_records.locations.business.retail.purchase.search

import java.time.OffsetDateTime
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineEntity
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PurchaseSearchQueryBuilderTest {

  private val orderedTotalRoundingPattern = Regex("""storage_units_per_user_provided_unit,\s*(\d+)\s*\)\)""")

  @BeforeEach
  fun setUp() {
    SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
  }

  @AfterEach
  fun clearSession() {
    SessionContextProvider.clear()
  }

  @Test
  fun `the amount filter rounds each line at the money scale`() {
    val predicate = QueryBuilderContext(params = mutableMapOf(PurchaseSearchParameterNames.MIN_AMOUNT to BigDecimal.ONE))

    val sql = PurchaseSearchQueryBuilder.buildListQuery(predicate, null).sql

    assertRoundsAtMoneyScale(sql)
  }

  @Test
  fun `both summaries round each line at the money scale`() {
    val predicate = QueryBuilderContext()

    assertRoundsAtMoneyScale(PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate).sql)
    assertRoundsAtMoneyScale(PurchaseSearchQueryBuilder.buildSupplierSummaryQuery(predicate).sql)
  }

  private fun cursor() = KeysetSearchCursor(OffsetDateTime.parse("2026-01-01T00:00:00Z"), UUID.randomUUID())

  @Test
  fun `the list is newest first by default and keysets with a less-than cursor`() {
    val sql = PurchaseSearchQueryBuilder.buildListQuery(QueryBuilderContext(), cursor()).sql

    assertTrue(sql.contains("created_on DESC") && sql.contains(".id DESC"), sql)
    assertTrue(sql.contains(") < ("), sql)
  }

  @Test
  fun `the list is oldest first with a greater-than cursor when asked`() {
    val sql = PurchaseSearchQueryBuilder.buildListQuery(QueryBuilderContext(), cursor(), oldestFirst = true).sql

    assertTrue(sql.contains("created_on ASC") && sql.contains(".id ASC"), sql)
    assertTrue(sql.contains(") > ("), sql)
  }

  @Test
  fun `a maximum amount alone also sums the ordered total`() {
    val predicate = QueryBuilderContext(params = mutableMapOf(PurchaseSearchParameterNames.MAX_AMOUNT to BigDecimal.TEN))

    val sql = PurchaseSearchQueryBuilder.buildListQuery(predicate, null).sql

    assertTrue(sql.contains("ordered_total"), sql)
  }

  @Test
  fun `an unfiltered list does not sum the ordered total`() {
    val sql = PurchaseSearchQueryBuilder.buildListQuery(QueryBuilderContext(), null).sql

    assertTrue(!sql.contains("ordered_total"), sql)
  }

  @Test
  fun `the SQL line total equals the entity total to the cent for fractional per-unit ratios`() {
    val unitCosts = listOf("10.0000", "1.0000", "0.0050", "3.3333", "7.7777", "123456.7891")
    val storageUnitsPerUserProvidedUnits = listOf(3L, 7L, 8L, 12L, 18L, 24L)
    val canceledStorageQuantities = listOf("0", "1", "2.5")

    unitCosts.forEach { unitCost ->
      storageUnitsPerUserProvidedUnits.forEach { storageUnitsPerUserProvidedUnit ->
        canceledStorageQuantities.forEach { canceledStorageQuantity ->
          val purchaseLineEntity = PurchaseLineEntity(
            purchaseId = UUID.randomUUID(),
            locationProductId = UUID.randomUUID(),
            userProvidedQuantityOrdered = BigDecimal("3"),
            unitCost = BigDecimal(unitCost),
            unitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
            storageQuantityCanceled = BigDecimal(canceledStorageQuantity)
          )
          val sqlLineTotal = Decimals.MoneyScale.round(
            Decimals.Scale10.divide(
              BigDecimal(unitCost).multiply(BigDecimal(3 * storageUnitsPerUserProvidedUnit).subtract(BigDecimal(canceledStorageQuantity))),
              BigDecimal(storageUnitsPerUserProvidedUnit)
            )
          )

          assertEquals(
            0,
            sqlLineTotal.compareTo(purchaseLineEntity.getTotalCost()),
            "unitCost=$unitCost units=$storageUnitsPerUserProvidedUnit canceled=$canceledStorageQuantity"
          )
        }
      }
    }
  }

  private fun assertRoundsAtMoneyScale(sql: String) {
    val roundingScale = orderedTotalRoundingPattern.find(sql)?.groupValues?.get(1)?.toInt()

    assertTrue(roundingScale == Decimals.MoneyScale.scale, "ordered_total rounds at $roundingScale, expected ${Decimals.MoneyScale.scale}: $sql")
  }

  @Test
  fun `every supplied filter adds its clause and its parameter`() {
    val parameters = PurchaseSearchParameters(
      recordedFrom = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
      recordedBefore = OffsetDateTime.parse("2026-02-01T00:00:00Z"),
      purchaseDateFrom = OffsetDateTime.parse("2026-01-05T00:00:00Z"),
      purchaseDateBefore = OffsetDateTime.parse("2026-01-20T00:00:00Z"),
      supplierIds = listOf(UUID.randomUUID()),
      purchaseStatuses = listOf(PurchaseStatus.ORDERED),
      paymentStatuses = listOf(PaymentStatus.UNPAID),
      minAmount = BigDecimal("1.00"),
      maxAmount = BigDecimal("9.00"),
      purchaseReferenceNumbers = listOf("PRCH-1")
    )

    val predicate = PurchaseSearchQueryBuilder.buildPredicate(parameters)

    val whereSql = predicate.whereClauses.joinToString(" AND ")
    listOf(
      ".created_on", ".date_ordered", ".supplier_id", ".purchase_status", ".payment_status", "ordered_total",
      ".reference_number"
    ).forEach { assertTrue(whereSql.contains(it), "missing $it in $whereSql") }
    listOf(
      PurchaseSearchParameterNames.RECORDED_FROM, PurchaseSearchParameterNames.RECORDED_BEFORE,
      PurchaseSearchParameterNames.PURCHASE_DATE_FROM, PurchaseSearchParameterNames.PURCHASE_DATE_BEFORE,
      PurchaseSearchParameterNames.SUPPLIER_IDS, PurchaseSearchParameterNames.PURCHASE_STATUSES,
      PurchaseSearchParameterNames.PAYMENT_STATUSES, PurchaseSearchParameterNames.MIN_AMOUNT,
      PurchaseSearchParameterNames.MAX_AMOUNT, PurchaseSearchParameterNames.PURCHASE_REFERENCE_NUMBERS
    ).forEach { assertTrue(predicate.params.containsKey(it), "missing parameter $it") }
  }

  @Test
  fun `no filter leaves only the always-true clause`() {
    val predicate = PurchaseSearchQueryBuilder.buildPredicate(PurchaseSearchParameters())

    assertEquals(listOf("1=1"), predicate.whereClauses)
    assertTrue(predicate.params.isEmpty())
  }

  @Test
  fun `both summaries join the active delivery lines to total what was delivered`() {
    val predicate = QueryBuilderContext()

    listOf(
      PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate).sql,
      PurchaseSearchQueryBuilder.buildSupplierSummaryQuery(predicate).sql
    ).forEach { assertTrue(it.contains("purchase_delivery_line"), it) }
  }
}
