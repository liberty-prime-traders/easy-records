package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.stock.api.read.StockMovementFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleCostRestatedEvent
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.UUID

class SaleCostRestatementHandlerForKafkaTest {

    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val saleCostRestatementHandlerForKafka = SaleCostRestatementHandlerForKafka(
        mock(StockRevisionRepository::class.java), mock(StockMovementFetcher::class.java), eventPublisher
    )
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

    private fun costChange(movementType: MovementType, oldCost: String, newCost: String) =
        ConsumerCostChange(movementType, "DOC1", "DET1", BigDecimal(oldCost), BigDecimal(newCost))

    @Test
    fun `only sales are totalled`() {
        saleCostRestatementHandlerForKafka.publish(
            stockRevision,
            listOf(
                costChange(MovementType.SALE, "75.4000", "74.1000"),
                costChange(MovementType.SALE, "72.5000", "71.2500"),
                costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "5.0000", "4.5000"),
            )
        )

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture() ?: Any())
        val event = eventCaptor.value as SaleCostRestatedEvent
        assertEquals(BigDecimal("147.90"), event.oldCostTotal.toBigDecimal())
        assertEquals(BigDecimal("145.35"), event.newCostTotal.toBigDecimal())
        assertEquals("STRV000001", event.stockRevisionReferenceNumber)
    }

    @Test
    fun `no restated sale cost publishes nothing`() {
        saleCostRestatementHandlerForKafka.publish(
            stockRevision, listOf(costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "5.0000", "4.5000"))
        )

        verify(eventPublisher, never()).publishEvent(any(Any::class.java) ?: Any())
    }
}
