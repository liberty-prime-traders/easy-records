package app.myliberty.easy_records.locations.business.retail.purchase.api

import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseAssembler
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseEntity
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineRepository
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseRepository
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

class PurchaseDataFetcherTest {

    private val purchaseRepository = mock(PurchaseRepository::class.java)
    private val purchaseLineRepository = mock(PurchaseLineRepository::class.java)
    private val purchaseAssembler = mock(PurchaseAssembler::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val purchaseDataFetcher = PurchaseDataFetcher(
        purchaseRepository, purchaseLineRepository, purchaseAssembler, entityAdvisoryLock
    )

    private fun purchaseWithId(purchaseId: UUID, supplierId: UUID = UUID.randomUUID()) = PurchaseEntity(
        supplierId = supplierId,
        purchaseStatus = PurchaseStatus.ORDERED,
        paymentStatus = PaymentStatus.UNPAID
    ).also {
        it.id = purchaseId
        it.referenceNumber = "PO-$purchaseId"
    }

    @Test
    fun `fetchTop above 1000 is rejected before any query`() {
        val exception = assertThrows(LibertyGenericException::class.java) { purchaseDataFetcher.fetchTop(1001) }

        assertEquals("Limit exceeds maximum of 1000", exception.message)
    }

    @Test
    fun `fetchTop defaults to the newest 10`() {
        val purchases = listOf(purchaseWithId(UUID.randomUUID()))
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdOn"))
        `when`(purchaseRepository.findTopN(pageable)).thenReturn(purchases)
        `when`(purchaseAssembler.buildResponses(purchases)).thenReturn(emptyList())

        purchaseDataFetcher.fetchTop(null)

        verify(purchaseRepository).findTopN(pageable)
    }

    @Test
    fun `fetchTop honors the requested limit of exactly 1000`() {
        `when`(purchaseRepository.findTopN(PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdOn"))))
            .thenReturn(emptyList())

        purchaseDataFetcher.fetchTop(1000)

        verify(purchaseRepository).findTopN(PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdOn")))
    }

    @Test
    fun `info by reference number carries the purchase facts`() {
        val purchaseId = UUID.randomUUID()
        val purchase = purchaseWithId(purchaseId)
        `when`(purchaseRepository.findByReferenceNumber("PO-1")).thenReturn(purchase)

        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoByReferenceNumber("PO-1")

        assertEquals(
            PurchaseDataFetcher.PurchaseInfo(
                purchaseId, "PO-$purchaseId", purchase.supplierId, PurchaseStatus.ORDERED, PaymentStatus.UNPAID
            ),
            purchaseInfo
        )
    }

    @Test
    fun `info by an unknown reference number names the reference`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDataFetcher.findPurchaseInfoByReferenceNumber("PO-404")
        }

        assertEquals("Purchase PO-404 not found", exception.message)
    }

    @Test
    fun `location product of a line comes from the line repository`() {
        val purchaseLineId = UUID.randomUUID()
        val locationProductId = UUID.randomUUID()
        `when`(purchaseLineRepository.findLocationProductIdById(purchaseLineId)).thenReturn(locationProductId)

        assertEquals(locationProductId, purchaseDataFetcher.getLocationProductIdOfLine(purchaseLineId))
    }

    @Test
    fun `location product of an unknown line names the line`() {
        val purchaseLineId = UUID.randomUUID()

        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDataFetcher.getLocationProductIdOfLine(purchaseLineId)
        }

        assertEquals("Purchase line $purchaseLineId not found", exception.message)
    }

    @Test
    fun `supplier of a purchase is read from the purchase`() {
        val purchaseId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        `when`(purchaseRepository.getReferenceById(purchaseId)).thenReturn(purchaseWithId(purchaseId, supplierId))

        assertEquals(supplierId, purchaseDataFetcher.getSupplierId(purchaseId))
    }

    @Test
    fun `info by ids is keyed by purchase id and info by one id returns that purchase`() {
        val purchaseId = UUID.randomUUID()
        val otherPurchaseId = UUID.randomUUID()
        val purchase = purchaseWithId(purchaseId)
        val otherPurchase = purchaseWithId(otherPurchaseId)
        `when`(purchaseRepository.findAllById(setOf(purchaseId, otherPurchaseId)))
            .thenReturn(listOf(purchase, otherPurchase))
        `when`(purchaseRepository.findAllById(setOf(purchaseId))).thenReturn(listOf(purchase))

        val purchaseInfoById = purchaseDataFetcher.findPurchaseInfoByIds(setOf(purchaseId, otherPurchaseId))

        assertEquals(setOf(purchaseId, otherPurchaseId), purchaseInfoById.keys)
        assertEquals("PO-$otherPurchaseId", purchaseInfoById.getValue(otherPurchaseId).referenceNumber)
        assertEquals(purchaseId, purchaseDataFetcher.findPurchaseInfoById(purchaseId).id)
    }

    @Test
    fun `locking a purchase takes the purchase advisory lock without loading it`() {
        val purchaseId = UUID.randomUUID()

        purchaseDataFetcher.lockPurchase(purchaseId)

        verify(entityAdvisoryLock).acquire(LockNamespaces.PURCHASE, purchaseId)
    }

    @Test
    fun `lock and get takes the lock then returns the purchase`() {
        val purchaseId = UUID.randomUUID()
        val purchase = purchaseWithId(purchaseId)
        `when`(purchaseRepository.getReferenceById(purchaseId)).thenReturn(purchase)

        assertSame(purchase, purchaseDataFetcher.lockAndGetPurchase(purchaseId))
        verify(entityAdvisoryLock).acquire(LockNamespaces.PURCHASE, purchaseId)
    }
}
