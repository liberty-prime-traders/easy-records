package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleTaxFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
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
    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleRepository = mock(SaleRepository::class.java)
    private val stockReserver = mock(StockReserver::class.java)
    private val saleAssembler = mock(SaleAssembler::class.java)
    private val saleStockUpdater = mock(SaleStockUpdater::class.java)
    private val saleVoidHandlerForKafka = mock(SaleVoidHandlerForKafka::class.java)
    private val saleVoidRepository = mock(SaleVoidRepository::class.java)
    private val saleTaxFinalizer = mock(SaleTaxFinalizer::class.java)
    private val saleSummary = mock(SaleSummary::class.java)

    private val saleUpdater = SaleUpdater(
        saleDataFetcher = saleDataFetcher,
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

    private fun sourceSale(status: SaleStatus): SaleEntity {
        val sourceSale = SaleEntity(contactId = UUID.randomUUID(), status = status)
        sourceSale.id = sourceSaleId
        sourceSale.referenceNumber = "SALE0001"
        `when`(saleDataFetcher.lockAndGetSale(sourceSaleId)).thenReturn(sourceSale)
        `when`(saleAssembler.buildSummary(sourceSale)).thenReturn(saleSummary)
        return sourceSale
    }

    @Test
    fun `a discarded source is left as it is, nothing is voided or released`() {
        val discardedSale = sourceSale(SaleStatus.DISCARDED)

        val summary = saleUpdater.voidSourceOfConsolidation(sourceSaleId, "SALE0009")

        assertEquals(saleSummary, summary)
        assertEquals(SaleStatus.DISCARDED, discardedSale.status)
        verifyNoInteractions(saleVoidRepository, stockReserver, saleStockUpdater, saleTaxFinalizer)
        verify(saleRepository, never()).save(any(SaleEntity::class.java))
    }

    @Test
    fun `a draft source is discarded and its reservations released without a void record`() {
        val draftSale = sourceSale(SaleStatus.DRAFT)

        saleUpdater.voidSourceOfConsolidation(sourceSaleId, "SALE0009")

        assertEquals(SaleStatus.DISCARDED, draftSale.status)
        verify(stockReserver).clearBySale(sourceSaleId)
        verifyNoInteractions(saleVoidRepository)
    }

    @Test
    fun `a confirmed source is voided with a reason that names the new sale`() {
        val confirmedSale = sourceSale(SaleStatus.CONFIRMED)
        `when`(saleVoidRepository.save(any(SaleVoidEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<SaleVoidEntity>(0).also { it.createdOn = OffsetDateTime.now() }
        }

        saleUpdater.voidSourceOfConsolidation(sourceSaleId, "SALE0009")

        val saleVoidEntityCaptor = ArgumentCaptor.forClass(SaleVoidEntity::class.java)
        verify(saleVoidRepository).save(saleVoidEntityCaptor.capture())
        assertEquals("Consolidated into SALE0009", saleVoidEntityCaptor.value.reason)
        assertEquals(sourceSaleId, saleVoidEntityCaptor.value.saleId)
        assertEquals(SaleStatus.VOIDED, confirmedSale.status)
        verify(saleStockUpdater).restoreStock("SALE0001")
    }

    @Test
    fun `the void reason names the consolidated sale reference`() {
        assertEquals("Consolidated into SALE0042", SaleUpdater.consolidationVoidReason("SALE0042"))
    }
}
