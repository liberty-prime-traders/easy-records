package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.ConfirmedSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentSyncer
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.util.UUID

class ConfirmedSalePersisterPreAllocatedReferenceTest {

    private val saleRepository = mock(SaleRepository::class.java)
    private val saleDataFetcher = mock(SaleDataFetcher::class.java)

    private val confirmedSalePersister = ConfirmedSalePersister(
        saleRepository = saleRepository,
        saleLineRepository = mock(SaleLineRepository::class.java),
        saleLineCogsRepository = mock(SaleLineCogsRepository::class.java),
        stockReserver = mock(StockReserver::class.java),
        saleStockUpdater = mock(SaleStockUpdater::class.java),
        saleConfirmedHandlerForKafka = mock(SaleConfirmedHandlerForKafka::class.java),
        saleDataFetcher = saleDataFetcher,
        fiscalPeriodService = mock(FiscalPeriodService::class.java),
        saleSaveFinalizer = mock(SaleSaveFinalizer::class.java),
        saleAdjustmentSyncer = mock(SaleAdjustmentSyncer::class.java),
        saleValidator = mock(SaleValidator::class.java),
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        saleTaxFinalizer = mock(SaleTaxFinalizer::class.java),
    )

    @Test
    fun `confirming refuses a pre-allocated reference before anything is loaded or saved`() {
        val saleSaveRequest = SaleSaveRequest(
            saleId = null,
            expectedVersion = null,
            contactId = UUID.randomUUID(),
            soldById = null,
            dateSold = null,
            notes = null,
            saleLines = emptyList(),
            saleAdjustments = emptyList(),
            salePayments = emptyList(),
            preAllocatedReferenceNumber = "SALE0009",
        )

        assertThrows(RtsGenericException::class.java) { confirmedSalePersister.confirm(saleSaveRequest) }

        verifyNoInteractions(saleRepository, saleDataFetcher)
    }
}
