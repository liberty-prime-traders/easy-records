package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.`when`
import java.util.UUID

class SaleLockerTest {

    private val saleRepository = mock(SaleRepository::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val saleLocker = SaleLocker(saleRepository, entityAdvisoryLock)
    private val smallerSaleId = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    private val largerSaleId = UUID.fromString("00000000-0000-0000-0000-00000000000b")

    private fun sale(saleId: UUID, status: SaleStatus = SaleStatus.DRAFT, version: Long = 0L): SaleEntity {
        val sale = SaleEntity(contactId = UUID.randomUUID(), status = status)
        sale.id = saleId
        sale.referenceNumber = "SALE-$saleId"
        sale.version = version
        return sale
    }

    @Test
    fun `locks are taken in id order whatever order the sales were asked for, before any sale is read`() {
        `when`(saleRepository.findAllById(listOf(largerSaleId, smallerSaleId)))
            .thenReturn(listOf(sale(largerSaleId), sale(smallerSaleId)))

        saleLocker.lockAndGetSales(listOf(largerSaleId, smallerSaleId))

        val locksBeforeRead = inOrder(entityAdvisoryLock, saleRepository)
        locksBeforeRead.verify(entityAdvisoryLock).acquire(LockNamespaces.SALE, smallerSaleId)
        locksBeforeRead.verify(entityAdvisoryLock).acquire(LockNamespaces.SALE, largerSaleId)
        locksBeforeRead.verify(saleRepository).findAllById(listOf(largerSaleId, smallerSaleId))
    }

    @Test
    fun `a sale that no longer exists is named in the refusal`() {
        `when`(saleRepository.findAllById(listOf(smallerSaleId, largerSaleId))).thenReturn(listOf(sale(smallerSaleId)))

        val refusal = assertThrows(LibertyGenericException::class.java) {
            saleLocker.lockAndGetSales(listOf(smallerSaleId, largerSaleId))
        }

        assertTrue(refusal.message.contains(largerSaleId.toString()))
    }

    @Test
    fun `a draft is loaded only at the version the caller opened`() {
        `when`(saleRepository.findAllById(listOf(smallerSaleId))).thenReturn(listOf(sale(smallerSaleId, version = 4L)))

        assertEquals(smallerSaleId, saleLocker.loadDraftAtVersion(smallerSaleId, 4L).id)
        assertThrows(LibertyGenericException::class.java) { saleLocker.loadDraftAtVersion(smallerSaleId, 3L) }
        assertThrows(LibertyGenericException::class.java) { saleLocker.loadDraftAtVersion(smallerSaleId, null) }
    }

    @Test
    fun `a sale that is not a draft is refused when loaded for a save`() {
        `when`(saleRepository.findAllById(listOf(smallerSaleId)))
            .thenReturn(listOf(sale(smallerSaleId, SaleStatus.CONFIRMED)))

        assertThrows(LibertyGenericException::class.java) { saleLocker.loadDraftAtVersion(smallerSaleId, 0L) }
    }
}
