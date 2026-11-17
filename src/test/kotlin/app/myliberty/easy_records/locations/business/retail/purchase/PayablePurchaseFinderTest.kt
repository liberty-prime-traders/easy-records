package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.bulk_payment.PurchaseBulkPaymentSearchDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.locations.business.retail.purchase.search.PurchaseSearchFetcher
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PayablePurchaseFinderTest {

  private val purchaseSearchFetcher = mock(PurchaseSearchFetcher::class.java)
  private val payablePurchaseFinder = PayablePurchaseFinder(purchaseSearchFetcher)
  private val supplierId = UUID.randomUUID()
  private val january = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

  @Suppress("UNCHECKED_CAST")
  private fun <T> anyNonNull(): T {
    ArgumentMatchers.any<T>()
    return null as T
  }

  private fun noCursor(): KeysetSearchCursor? = ArgumentMatchers.isNull<KeysetSearchCursor>()

  private fun searchDto(orderedFrom: OffsetDateTime, orderedBefore: OffsetDateTime) =
    PurchaseBulkPaymentSearchDto(supplierId, orderedFrom, orderedBefore)

  @Test
  fun `asks for the supplier's unpaid orders in the range, oldest first, capped at the payable limit`() {
    val searchedParameters = mutableListOf<PurchaseSearchParameters>()
    val searchedSizes = mutableListOf<Int>()
    val searchedOldestFirst = mutableListOf<Boolean>()
    doAnswer { invocation ->
      searchedParameters.add(invocation.getArgument(0))
      searchedSizes.add(invocation.getArgument(2))
      searchedOldestFirst.add(invocation.getArgument(3))
      emptyList<PurchaseEntity>()
    }.`when`(purchaseSearchFetcher).search(anyNonNull(), noCursor(), anyInt(), anyBoolean())

    payablePurchaseFinder.findPayablePurchases(searchDto(january, january.plusMonths(1)))

    val parameters = searchedParameters.single()
    assertEquals(listOf(supplierId), parameters.supplierIds)
    assertEquals(january, parameters.purchaseDateFrom)
    assertEquals(january.plusMonths(1), parameters.purchaseDateBefore)
    assertEquals(PayablePurchaseFinder.PAYABLE_PURCHASE_STATUSES, parameters.purchaseStatuses)
    assertFalse(PurchaseStatus.DRAFT in parameters.purchaseStatuses)
    assertFalse(PurchaseStatus.CANCELED in parameters.purchaseStatuses)
    assertEquals(listOf(PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_SETTLED), parameters.paymentStatuses)
    assertEquals(listOf(PayablePurchaseFinder.MAX_PAYABLE_RESULTS), searchedSizes)
    assertTrue(searchedOldestFirst.single())
  }

  @Test
  fun `needs a range of at most a year, in order, before searching`() {
    assertThrows<LibertyGenericException> {
      payablePurchaseFinder.findPayablePurchases(searchDto(january, january.plusYears(1).plusDays(1)))
    }
    assertThrows<LibertyGenericException> {
      payablePurchaseFinder.findPayablePurchases(searchDto(january.plusDays(2), january.plusDays(1)))
    }
    verify(purchaseSearchFetcher, never()).search(anyNonNull(), noCursor(), anyInt(), anyBoolean())
  }
}
