package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.delivery.api.DeliveryLineRevisionTotalsDto
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockEntryDto
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockEntryFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class StockRevisionHandlerForKafkaTest {

    private val stockRevisionRepository = mock(StockRevisionRepository::class.java)
    private val stockEntryFetcher = mock(StockEntryFetcher::class.java)
    private val purchaseDeliveryDataFetcher = mock(PurchaseDeliveryDataFetcher::class.java)
    private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val stockRevisionHandlerForKafka = StockRevisionHandlerForKafka(
        stockRevisionRepository, stockEntryFetcher, purchaseDeliveryDataFetcher, purchaseDataFetcher, eventPublisher
    )

    private val purchaseId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()
    private val stockRevision = StockRevisionEntity(
        sourceType = StockRevisionSourceType.DELIVERY_LINE,
        sourceReferenceNumber = "DELN000002",
        revisedStockEntryId = UUID.randomUUID(),
        replacementStockEntryId = UUID.randomUUID(),
        reason = "Supplier corrected the invoice"
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "STRV000001"
    }

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun givenPurchase() {
        val purchaseInfo = mock(PurchaseDataFetcher.PurchaseInfo::class.java)
        `when`(purchaseInfo.referenceNumber).thenReturn("PRCH1")
        `when`(purchaseInfo.supplierId).thenReturn(supplierId)
        `when`(purchaseDataFetcher.findPurchaseInfoById(purchaseId)).thenReturn(purchaseInfo)
    }

    @Test
    fun `an unchanged line total publishes nothing`() {
        stockRevisionHandlerForKafka.publish(stockRevision, purchaseId, BigDecimal("50.00"), BigDecimal("50.00"))

        verify(eventPublisher, never()).publishEvent(any(Any::class.java) ?: Any())
    }

    @Test
    fun `a reissue rebuilds the event from the revised line and the replacement line`() {
        givenPurchase()
        `when`(stockRevisionRepository.findById(stockRevision.id!!)).thenReturn(Optional.of(stockRevision))
        val revisedStockEntry = mock(StockEntryDto::class.java)
        `when`(revisedStockEntry.detailReference).thenReturn("DELN000001")
        `when`(stockEntryFetcher.get(stockRevision.revisedStockEntryId)).thenReturn(revisedStockEntry)
        `when`(purchaseDeliveryDataFetcher.getRevisionTotals("DELN000001", "DELN000002")).thenReturn(
            DeliveryLineRevisionTotalsDto(purchaseId, BigDecimal("100.00"), BigDecimal("26.00"))
        )

        stockRevisionHandlerForKafka.reissue(stockRevision.id!!)

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture() ?: Any())
        val event = eventCaptor.value as PurchaseDeliveryRevisedEvent
        assertEquals("STRV000001", event.stockRevisionReferenceNumber)
        assertEquals(BigDecimal("100.00"), event.revisedLineTotal.toBigDecimal())
        assertEquals(BigDecimal("26.00"), event.replacementLineTotal.toBigDecimal())
        assertTrue(event.supplierId == supplierId)
    }
}
