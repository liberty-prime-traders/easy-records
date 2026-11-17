package app.myliberty.easy_records.locations.business.product_purchases_report

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLinesParameters
import app.myliberty.easy_records.locations.business.product_purchases_report.api.ProductPurchasesReportService
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.organizations.business.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactIdentity
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.unitconversion.api.QuantityBreakdownFactory
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ProductPurchasesReportServiceDeliveryLinesTest {

  private val productPurchasesReportFetcher = mock(ProductPurchasesReportFetcher::class.java)
  private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
  private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
  private val quantityBreakdownFactory = mock(QuantityBreakdownFactory::class.java)
  private val contactService = mock(ContactService::class.java)
  private val productCategoryService = mock(ProductCategoryService::class.java)

  private val service = ProductPurchasesReportService(
    productPurchasesReportFetcher,
    locationProductDataFetcher,
    unitConversionGraphFacade,
    quantityBreakdownFactory,
    contactService,
    productCategoryService
  )

  private val locationProductId = UUID.randomUUID()
  private val baseUnitId = UUID.randomUUID()
  private val supplierId = UUID.randomUUID()
  private val from = OffsetDateTime.parse("2026-10-01T00:00:00+00:00")
  private val before = OffsetDateTime.parse("2026-10-08T00:00:00+00:00")
  private val range = ProductPurchasesDeliveredRange(from, before)
  private val deliveryLinesParameters = ProductPurchasesDeliveryLinesParameters(from, before)

  @BeforeEach
  fun stubCollaborators() {
    `when`(locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))).thenReturn(
      mapOf(
        locationProductId to LocationProductSummaryDto(
          locationProductId, "LP000001", UUID.randomUUID(), "Rice", "Grains", baseUnitId
        )
      )
    )
    `when`(quantityBreakdownFactory.forBaseUnits(listOf(baseUnitId))).thenReturn(
      mapOf(
        baseUnitId to QuantityBreakdown.forBaseUnit(
          listOf(BreakdownUnit(baseUnitId, "Piece", 1, baseUnitId, OffsetDateTime.now())),
          baseUnitId
        )
      )
    )
    val graph = mock(UnitConversionGraph::class.java)
    `when`(graph.getSizeInRoot(baseUnitId)).thenReturn(1L)
    `when`(unitConversionGraphFacade.getOrLoad()).thenReturn(graph)

    val contact = mock(ContactDto::class.java)
    `when`(contact.id).thenReturn(supplierId)
    `when`(contact.identity).thenReturn(ContactIdentity.Organization("Kisumu Traders"))
    `when`(contactService.getAllContactDtos()).thenReturn(listOf(contact))
  }

  @Test
  fun `a full page with one extra row reports more and cursors on the last row kept`() {
    val rows = listOf(rawRow(from.plusDays(3)), rawRow(from.plusDays(2)), rawRow(from.plusDays(1)))
    `when`(productPurchasesReportFetcher.fetchDeliveryLines(locationProductId, deliveryLinesParameters, range, null, 2))
      .thenReturn(rows)

    val page = service.searchDeliveryLines(locationProductId, pageRequest(previousCursor = "", requestedSize = 2))

    assertTrue(page.hasMore)
    assertEquals(listOf(rows[0].id, rows[1].id), page.contents.map { it.id })
    assertEquals(KeysetSearchCursor(rows[1].deliveredAt, rows[1].id).encode(), page.currentCursor)
  }

  @Test
  fun `a short page reports no more rows`() {
    val rows = listOf(rawRow(from.plusDays(2)), rawRow(from.plusDays(1)))
    `when`(productPurchasesReportFetcher.fetchDeliveryLines(locationProductId, deliveryLinesParameters, range, null, 2))
      .thenReturn(rows)

    val page = service.searchDeliveryLines(locationProductId, pageRequest(previousCursor = "", requestedSize = 2))

    assertFalse(page.hasMore)
    assertEquals(2, page.contents.size)
    assertEquals(KeysetSearchCursor(rows[1].deliveredAt, rows[1].id).encode(), page.currentCursor)
  }

  @Test
  fun `an empty page keeps the cursor it was given`() {
    val previousCursor = KeysetSearchCursor(from.plusDays(1), UUID.randomUUID())
    `when`(
      productPurchasesReportFetcher.fetchDeliveryLines(
        locationProductId,
        deliveryLinesParameters,
        range,
        previousCursor,
        2
      )
    ).thenReturn(emptyList())

    val page = service.searchDeliveryLines(
      locationProductId,
      pageRequest(previousCursor = previousCursor.encode(), requestedSize = 2)
    )

    assertFalse(page.hasMore)
    assertTrue(page.contents.isEmpty())
    assertEquals(previousCursor.encode(), page.currentCursor)
  }

  private fun pageRequest(previousCursor: String, requestedSize: Int) = PageRequest(
    previousCursor = previousCursor,
    requestedSize = requestedSize,
    parameters = deliveryLinesParameters
  )

  private fun rawRow(deliveredAt: OffsetDateTime) = ProductPurchasesDeliveryLineRawRow(
    id = UUID.randomUUID(),
    purchaseReferenceNumber = "PO000001",
    deliveryReferenceNumber = "DL000001",
    deliveredAt = deliveredAt,
    supplierId = supplierId,
    unitId = baseUnitId,
    userProvidedQuantityDelivered = BigDecimal("1"),
    storageUnitsPerUserProvidedUnit = 1,
    unitCost = BigDecimal("1.00"),
    status = PurchaseDeliveryLineStatus.ACTIVE
  )
}
