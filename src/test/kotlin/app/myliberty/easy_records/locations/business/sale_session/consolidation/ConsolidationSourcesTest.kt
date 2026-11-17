package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
