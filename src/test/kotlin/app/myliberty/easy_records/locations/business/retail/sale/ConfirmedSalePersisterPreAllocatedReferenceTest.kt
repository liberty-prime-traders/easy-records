package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.ConfirmedSalePersister
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentSyncer
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.SaleStockUpdater
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.util.UUID

class ConfirmedSalePersisterPreAllocatedReferenceTest {

    private val saleRepository = mock(SaleRepository::class.java)
    private val saleLocker = mock(SaleLocker::class.java)

    private val confirmedSalePersister = ConfirmedSalePersister(
        saleRepository = saleRepository,
        saleLineRepository = mock(SaleLineRepository::class.java),
        saleLineCogsRepository = mock(SaleLineCogsRepository::class.java),
        stockReserver = mock(StockReserver::class.java),
        saleStockUpdater = mock(SaleStockUpdater::class.java),
        saleConfirmedHandlerForKafka = mock(SaleConfirmedHandlerForKafka::class.java),
        saleLocker = saleLocker,
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

        assertThrows(LibertyGenericException::class.java) { confirmedSalePersister.confirm(saleSaveRequest) }

        verifyNoInteractions(saleRepository, saleLocker)
    }
}
