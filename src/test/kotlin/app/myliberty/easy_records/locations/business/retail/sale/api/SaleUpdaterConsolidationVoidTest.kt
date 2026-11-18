package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import app.myliberty.easy_records.locations.business.retail.sale.SaleAssembler
import app.myliberty.easy_records.locations.business.retail.sale.SaleEntity
import app.myliberty.easy_records.locations.business.retail.sale.SaleLocker
import app.myliberty.easy_records.locations.business.retail.sale.SaleRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleTaxFinalizer
import app.myliberty.easy_records.locations.business.retail.sale.SaleValidator
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidEntity
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidRepository
import app.myliberty.easy_records.locations.business.retail.sale.VoidedSale
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.SaleStockUpdater
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class SaleUpdaterConsolidationVoidTest {

    private val sourceSaleId = UUID.randomUUID()
    private val saleLocker = mock(SaleLocker::class.java)
    private val saleRepository = mock(SaleRepository::class.java)
    private val stockReserver = mock(StockReserver::class.java)
    private val saleAssembler = mock(SaleAssembler::class.java)
    private val saleStockUpdater = mock(SaleStockUpdater::class.java)
    private val saleVoidHandlerForKafka = mock(SaleVoidHandlerForKafka::class.java)
    private val saleVoidRepository = mock(SaleVoidRepository::class.java)
    private val saleTaxFinalizer = mock(SaleTaxFinalizer::class.java)

    private val saleUpdater = SaleUpdater(
        saleLocker = saleLocker,
        saleRepository = saleRepository,
        stockReserver = stockReserver,
        saleAssembler = saleAssembler,
        saleStockUpdater = saleStockUpdater,
        saleVoidHandlerForKafka = saleVoidHandlerForKafka,
        saleValidator = mock(SaleValidator::class.java),
        saleVoidRepository = saleVoidRepository,
        fiscalPeriodService = mock(FiscalPeriodService::class.java),
        saleTaxFinalizer = saleTaxFinalizer,
    )

    private fun sourceSale(
        status: SaleStatus,
        saleId: UUID = sourceSaleId,
        reference: String = "SALE0001",
    ): SaleEntity {
        val sourceSale = SaleEntity(contactId = UUID.randomUUID(), status = status)
        sourceSale.id = saleId
        sourceSale.referenceNumber = reference
        return sourceSale
    }

    private fun lockReturns(vararg sourceSales: SaleEntity) {
        `when`(saleLocker.lockAndGetSales(sourceSales.map { it.id!! })).thenReturn(sourceSales.toList())
    }

    private val savedSaleVoidEntities = mutableListOf<SaleVoidEntity>()

    private fun voidRepositoryStampsCreatedOn() {
        `when`(saleVoidRepository.saveAll(anyList<SaleVoidEntity>())).thenAnswer { invocation ->
            invocation.getArgument<List<SaleVoidEntity>>(0)
                .onEach { it.createdOn = OffsetDateTime.now() }
                .also { savedSaleVoidEntities.addAll(it) }
        }
    }

    @Test
    fun `a draft source is discarded and its reservations released without a void record`() {
        val draftSale = sourceSale(SaleStatus.DRAFT)
        lockReturns(draftSale)

        saleUpdater.voidSourcesOfConsolidation(listOf(sourceSaleId), "SALE0009")

        assertEquals(SaleStatus.DISCARDED, draftSale.status)
        verify(stockReserver).clearBySales(listOf(sourceSaleId))
        verifyNoInteractions(saleVoidRepository)
    }

    @Test
    fun `a discarded draft keeps its reason after its own notes, and a blank reason adds nothing`() {
        val draftWithNotes = sourceSale(SaleStatus.DRAFT)
        draftWithNotes.notes = "deliver Monday"
        val draftWithoutNotes = sourceSale(SaleStatus.DRAFT, UUID.randomUUID(), "SALE0002")
        lockReturns(draftWithNotes, draftWithoutNotes)

        saleUpdater.voidSourcesOfConsolidation(listOf(sourceSaleId, draftWithoutNotes.id!!), "SALE0009")
        val discardedBlank = sourceSale(SaleStatus.DRAFT, UUID.randomUUID(), "SALE0003")
        `when`(saleLocker.lockAndGetSale(discardedBlank.id!!)).thenReturn(discardedBlank)
        saleUpdater.voidSale(SaleVoidCreateDto(discardedBlank.id!!, "  "))

        assertEquals("deliver Monday\nDiscarded: Consolidated into SALE0009", draftWithNotes.notes)
        assertEquals("Discarded: Consolidated into SALE0009", draftWithoutNotes.notes)
        assertNull(discardedBlank.notes)
    }

    @Test
    fun `notes are written only at the version the caller opened`() {
        val sale = sourceSale(SaleStatus.DRAFT)
        sale.notes = "first"
        `when`(saleLocker.lockAndGetSale(sourceSaleId)).thenReturn(sale)

        assertThrows(LibertyGenericException::class.java) {
            saleUpdater.updateNotesAtVersion(sourceSaleId, "from a stale tab", sale.version + 1)
        }
        assertThrows(LibertyGenericException::class.java) {
            saleUpdater.updateNotesAtVersion(sourceSaleId, "from a tab with no version", null)
        }

        assertEquals("first", sale.notes)
        verify(saleRepository, never()).saveAndFlush(sale)
    }

    @Test
    fun `a confirmed source is voided with a reason that names the new sale`() {
        val confirmedSale = sourceSale(SaleStatus.CONFIRMED)
        lockReturns(confirmedSale)
        voidRepositoryStampsCreatedOn()

        saleUpdater.voidSourcesOfConsolidation(listOf(sourceSaleId), "SALE0009")

        val saleVoidEntity = savedSaleVoidEntities.single()
        assertEquals("Consolidated into SALE0009", saleVoidEntity.reason)
        assertEquals(sourceSaleId, saleVoidEntity.saleId)
        assertEquals(SaleStatus.VOIDED, confirmedSale.status)
        verify(saleStockUpdater).restoreStock("SALE0001")
    }

    @Test
    fun `several sources are voided together and tax is finalized in one call`() {
        val draftSale = sourceSale(SaleStatus.DRAFT, UUID.randomUUID(), "SALE0002")
        val confirmedSale = sourceSale(SaleStatus.CONFIRMED, UUID.randomUUID(), "SALE0003")
        lockReturns(draftSale, confirmedSale)
        voidRepositoryStampsCreatedOn()

        saleUpdater.voidSourcesOfConsolidation(listOf(draftSale.id!!, confirmedSale.id!!), "SALE0009")

        assertEquals(SaleStatus.DISCARDED, draftSale.status)
        assertEquals(SaleStatus.VOIDED, confirmedSale.status)
        verify(stockReserver).clearBySales(listOf(draftSale.id!!))
        verify(saleStockUpdater).restoreStock("SALE0003")
        verify(saleStockUpdater, never()).restoreStock("SALE0002")
        verify(saleRepository).saveAll(listOf(draftSale, confirmedSale))
        verify(saleTaxFinalizer).finalizeTaxForVoids(anyList<VoidedSale>())
    }

    @Test
    fun `the void reason names the consolidated sale reference`() {
        assertEquals("Consolidated into SALE0042", SaleUpdater.consolidationVoidReason("SALE0042"))
    }
}
