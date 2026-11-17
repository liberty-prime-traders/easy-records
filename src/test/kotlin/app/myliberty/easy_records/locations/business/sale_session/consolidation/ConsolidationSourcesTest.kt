package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionValidator
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.util.UUID

class ConsolidationSourcesTest {

    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val consolidationSources = ConsolidationSources(
        saleDataFetcher,
        SaleSessionValidator(mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)),
    )

    @Test
    fun `too many sales are refused before a single lock is taken`() {
        assertThrows(RtsGenericException::class.java) { consolidationSources.lock(List(21) { UUID.randomUUID() }) }

        verifyNoInteractions(saleDataFetcher)
    }
}
