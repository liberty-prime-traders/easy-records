package app.myliberty.easy_records.locations.business.retail.purchase.search

import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseEntity
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.queries.SqlQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseSearchFetcherTest {

  private val purchaseSearchListExecutor = mock(PurchaseSearchListExecutor::class.java)
  private val purchaseSearchFetcher = PurchaseSearchFetcher(
    purchaseSearchListExecutor, mock(PurchaseSearchSummaryExecutor::class.java)
  )
  private val executedQueries = mutableListOf<SqlQuery>()
  private val executedPageSizes = mutableListOf<Int>()


  private fun givenEmptyResults() {
    doAnswer { invocation ->
      executedQueries.add(invocation.getArgument(0))
      executedPageSizes.add(invocation.getArgument(1))
      emptyList<Any>()
    }.`when`(purchaseSearchListExecutor).execute(anyNonNull(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())
  }

  @Test
  fun `a search is newest first unless asked otherwise, and reads one row past the page to detect more`() {
    givenEmptyResults()

    purchaseSearchFetcher.search(PurchaseSearchParameters(), null, 10)

    assertEquals(listOf(11), executedPageSizes)
    assertTrue(executedQueries.single().sql.contains("created_on DESC"), executedQueries.single().sql)
  }

  @Test
  fun `a search returns the rows the executor read, including the one past the page`() {
    val readPurchases = List(2) {
      PurchaseEntity(
        supplierId = UUID.randomUUID(), purchaseStatus = PurchaseStatus.ORDERED,
        paymentStatus = PaymentStatus.UNPAID, dateOrdered = OffsetDateTime.parse("2026-01-01T00:00:00Z")
      )
    }
    doAnswer { readPurchases }.`when`(purchaseSearchListExecutor)
      .execute(anyNonNull(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())

    val searchedPurchases = purchaseSearchFetcher.search(PurchaseSearchParameters(), null, 1)

    assertEquals(readPurchases, searchedPurchases)
  }

  @Test
  fun `a search asked for oldest first orders ascending`() {
    givenEmptyResults()

    purchaseSearchFetcher.search(PurchaseSearchParameters(), null, 10, oldestFirst = true)

    assertTrue(executedQueries.single().sql.contains("created_on ASC"), executedQueries.single().sql)
  }
}
