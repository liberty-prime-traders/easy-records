package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.location_product.api.LastPurchaseCostUpdateDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryStatus
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.PurchaseDeliveryStockUpdater
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredLineDto
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class PurchaseDeliveryInventoryProcessorTest {

    private val deliveryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val locationProductService = mock(LocationProductService::class.java)
    private val purchaseDeliveryStockUpdater = mock(PurchaseDeliveryStockUpdater::class.java)
    private val deliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryInventoryProcessor = PurchaseDeliveryInventoryProcessor(
        locationProductService,
        purchaseDeliveryStockUpdater,
        deliveryRepository
    )

    private fun deliveryWithStatus(status: PurchaseDeliveryStatus) = PurchaseDeliveryEntity(
        purchaseId = UUID.randomUUID(),
        status = status,
        deliveredAt = OffsetDateTime.now()
    ).also { it.id = deliveryId }

    private fun purchaseDeliveredEvent() = PurchaseDeliveredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1"),
        timestamp = Instant.now(),
        correlationId = null,
        purchaseId = UUID.randomUUID(),
        purchaseReferenceNumber = "PCH000001",
        deliveryId = deliveryId,
        deliveryReferenceNumber = "DEL000001",
        deliveredAt = OffsetDateTime.now(),
        supplierId = UUID.randomUUID(),
        lines = listOf(
            PurchaseDeliveredLineDto(
                deliveryLineId = UUID.randomUUID(),
                lineReferenceNumber = "DELN000001",
                locationProductId = locationProductId,
                userProvidedQuantityDelivered = BigDecimal("2"),
                unitId = UUID.randomUUID(),
                storageUnitsPerUserProvidedUnit = 12L,
                unitCost = BigDecimal("60.00")
            )
        ),
        deliveryTotal = Money.of(BigDecimal("120.00"))
    )

    @Test
    fun `a delivery still processing is applied to stock`() {
        `when`(deliveryRepository.findById(deliveryId))
            .thenReturn(Optional.of(deliveryWithStatus(PurchaseDeliveryStatus.PROCESSING)))

        assertTrue(purchaseDeliveryInventoryProcessor.shouldProcess(purchaseDeliveredEvent()))
    }

    @Test
    fun `a delivery already received is skipped so a retried event does not stock twice`() {
        `when`(deliveryRepository.findById(deliveryId))
            .thenReturn(Optional.of(deliveryWithStatus(PurchaseDeliveryStatus.RECEIVED)))

        assertFalse(purchaseDeliveryInventoryProcessor.shouldProcess(purchaseDeliveredEvent()))
    }

    @Test
    fun `an event for a delivery that no longer exists is skipped`() {
        `when`(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty())

        assertFalse(purchaseDeliveryInventoryProcessor.shouldProcess(purchaseDeliveredEvent()))
    }

    @Test
    fun `handling stocks the delivery, refreshes the last cost per product and marks the delivery received`() {
        val processingDelivery = deliveryWithStatus(PurchaseDeliveryStatus.PROCESSING)
        `when`(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(processingDelivery))
        val event = purchaseDeliveredEvent()

        purchaseDeliveryInventoryProcessor.handle(event)

        verify(purchaseDeliveryStockUpdater).recordPurchaseDelivery(event)
        verify(locationProductService).updateLastPurchaseCosts(
            listOf(LastPurchaseCostUpdateDto(locationProductId, BigDecimal("60.00"), 12L))
        )
        assertEquals(PurchaseDeliveryStatus.RECEIVED, processingDelivery.status)
        verify(deliveryRepository).save(processingDelivery)
    }

    @Test
    fun `handling a delivery that cannot be found refuses and leaves the status alone`() {
        `when`(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty())

        assertThrows<UpdatingNonExistingRecordException> {
            purchaseDeliveryInventoryProcessor.handle(purchaseDeliveredEvent())
        }

        verify(deliveryRepository, never()).save(anyNonNull(PurchaseDeliveryEntity::class.java))
    }
}
