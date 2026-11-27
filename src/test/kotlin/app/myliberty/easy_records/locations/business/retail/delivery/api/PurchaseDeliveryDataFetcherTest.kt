package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryAssembler
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryEntity
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineEntity
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryRepository
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineOwners
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryDataFetcherTest {

    private val purchaseLineId = UUID.randomUUID()
    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val purchaseDeliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryDataFetcher = PurchaseDeliveryDataFetcher(
        purchaseDeliveryRepository,
        purchaseDeliveryLineRepository,
        mock(PurchaseDeliveryAssembler::class.java)
    )

    private fun deliveryLine(
        userProvidedQuantity: String,
        storageUnitsPerUserProvidedUnit: Long,
        purchaseDeliveryId: UUID = UUID.randomUUID()
    ) =
        PurchaseDeliveryLineEntity(
            purchaseDeliveryId = purchaseDeliveryId,
            purchaseLineId = purchaseLineId,
            userProvidedQuantityDelivered = BigDecimal(userProvidedQuantity),
            unitCost = BigDecimal("10.00"),
            unitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        )

    @Test
    fun `the delivered quantity sums the active lines in storage units`() {
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseLineIdAndStatus(
                purchaseLineId, PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 12L), deliveryLine("5", 1L)))

        val delivered = purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(purchaseLineId)

        assertEquals(ExactQuantity.ofStorage(BigDecimal("29")), delivered)
    }

    @Test
    fun `a delivery total counts the active lines only`() {
        val deliveryId = UUID.randomUUID()
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseDeliveryIdInAndStatus(
                listOf(deliveryId), PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 1L), deliveryLine("3", 1L)))

        assertEquals(
            0, BigDecimal("50.00").compareTo(purchaseDeliveryDataFetcher.calculateSingleDeliveryTotal(deliveryId))
        )
    }

    @Test
    fun `a purchase total counts the active lines only`() {
        val purchaseId = UUID.randomUUID()
        val deliveryId = UUID.randomUUID()
        `when`(purchaseDeliveryRepository.findByPurchaseIdIn(setOf(purchaseId))).thenReturn(
            listOf(PurchaseDeliveryEntity(purchaseId, deliveredAt = OffsetDateTime.now()).also { it.id = deliveryId })
        )
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseDeliveryIdInAndStatus(
                listOf(deliveryId), PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 1L, deliveryId)))

        val deliveredTotals = purchaseDeliveryDataFetcher.calculateDeliveredTotals(setOf(purchaseId))

        assertEquals(0, BigDecimal("20.00").compareTo(deliveredTotals.getValue(purchaseId)))
    }

    @Test
    fun `a line with no active deliveries has delivered nothing`() {
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseLineIdAndStatus(
                purchaseLineId, PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(emptyList())

        assertEquals(ExactQuantity.ZERO, purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(purchaseLineId))
    }

    private fun referencedLine(referenceNumber: String, quantity: String, purchaseDeliveryId: UUID) =
        deliveryLine(quantity, 1L, purchaseDeliveryId).also { it.referenceNumber = referenceNumber }

    @Test
    fun `revision totals carry the purchase of the replacement line and both line totals`() {
        val purchaseId = UUID.randomUUID()
        val deliveryId = UUID.randomUUID()
        `when`(purchaseDeliveryLineRepository.findByReferenceNumberIn(listOf("DELN000001", "DELN000002")))
            .thenReturn(
                listOf(
                    referencedLine("DELN000002", "3", deliveryId),
                    referencedLine("DELN000001", "2", deliveryId)
                )
            )
        `when`(purchaseDeliveryRepository.getReferenceById(deliveryId))
            .thenReturn(PurchaseDeliveryEntity(purchaseId, deliveredAt = OffsetDateTime.now()))

        val totals = purchaseDeliveryDataFetcher.getRevisionTotals("DELN000001", "DELN000002")

        assertEquals(purchaseId, totals.purchaseId)
        assertEquals(0, BigDecimal("20.00").compareTo(totals.revisedLineTotal))
        assertEquals(0, BigDecimal("30.00").compareTo(totals.replacementLineTotal))
    }

    @Test
    fun `revision totals refuse a revised line that is not found`() {
        `when`(purchaseDeliveryLineRepository.findByReferenceNumberIn(listOf("DELN000001", "DELN000002")))
            .thenReturn(listOf(referencedLine("DELN000002", "3", UUID.randomUUID())))

        val refusal = assertThrows<LibertyGenericException> {
            purchaseDeliveryDataFetcher.getRevisionTotals("DELN000001", "DELN000002")
        }

        assertTrue(refusal.message.contains("Delivery line DELN000001 not found"))
    }

    @Test
    fun `revision totals refuse a replacement line that is not found`() {
        `when`(purchaseDeliveryLineRepository.findByReferenceNumberIn(listOf("DELN000001", "DELN000002")))
            .thenReturn(listOf(referencedLine("DELN000001", "2", UUID.randomUUID())))

        val refusal = assertThrows<LibertyGenericException> {
            purchaseDeliveryDataFetcher.getRevisionTotals("DELN000001", "DELN000002")
        }

        assertTrue(refusal.message.contains("Delivery line DELN000002 not found"))
    }

    @Test
    fun `the owners of a delivery line are its purchase and purchase line`() {
        val deliveryLineId = UUID.randomUUID()
        val purchaseId = UUID.randomUUID()
        val owners = mock(PurchaseDeliveryLineOwners::class.java)
        `when`(owners.purchaseId).thenReturn(purchaseId)
        `when`(owners.purchaseLineId).thenReturn(purchaseLineId)
        `when`(purchaseDeliveryLineRepository.findOwnersById(deliveryLineId)).thenReturn(owners)

        val ownersDto = purchaseDeliveryDataFetcher.getOwners(deliveryLineId)

        assertEquals(DeliveryLineOwnersDto(purchaseId, purchaseLineId), ownersDto)
    }

    @Test
    fun `the owners of a delivery line that is not found are refused with a refresh hint`() {
        val refusal = assertThrows<LibertyGenericException> {
            purchaseDeliveryDataFetcher.getOwners(UUID.randomUUID())
        }

        assertTrue(refusal.message.contains("That delivery line could not be found"))
    }

    @Test
    fun `delivery reference numbers are keyed by delivery id and skip deliveries without one`() {
        val referencedDeliveryId = UUID.randomUUID()
        val unreferencedDeliveryId = UUID.randomUUID()
        val referencedDelivery = PurchaseDeliveryEntity(UUID.randomUUID(), deliveredAt = OffsetDateTime.now())
            .also {
                it.id = referencedDeliveryId
                it.referenceNumber = "DEL000001"
            }
        val unreferencedDelivery = PurchaseDeliveryEntity(UUID.randomUUID(), deliveredAt = OffsetDateTime.now())
            .also { it.id = unreferencedDeliveryId }
        `when`(purchaseDeliveryRepository.findAllById(listOf(referencedDeliveryId, unreferencedDeliveryId)))
            .thenReturn(listOf(referencedDelivery, unreferencedDelivery))

        val referenceById = purchaseDeliveryDataFetcher
            .getDeliveryReferenceNumbersById(listOf(referencedDeliveryId, unreferencedDeliveryId))

        assertEquals(mapOf(referencedDeliveryId to "DEL000001"), referenceById)
    }

    @Test
    fun `a purchase with no deliveries has delivered a total of zero`() {
        val purchaseId = UUID.randomUUID()
        `when`(purchaseDeliveryRepository.findByPurchaseIdIn(setOf(purchaseId))).thenReturn(emptyList())

        val deliveredTotals = purchaseDeliveryDataFetcher.calculateDeliveredTotals(setOf(purchaseId))

        assertEquals(0, BigDecimal.ZERO.compareTo(deliveredTotals.getValue(purchaseId)))
    }
}
