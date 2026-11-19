package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
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
        assertThrows(LibertyGenericException::class.java) { consolidationSources.lock(List(21) { UUID.randomUUID() }) }

        verifyNoInteractions(saleDataFetcher)
    }
}
