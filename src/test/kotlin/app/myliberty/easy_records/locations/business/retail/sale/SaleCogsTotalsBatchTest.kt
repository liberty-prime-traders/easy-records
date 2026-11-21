package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineCogsStatus
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleCogsTotalsBatchTest {

    private val saleLineCogsRepository = mock(SaleLineCogsRepository::class.java)
    private val saleDataFetcher = SaleDataFetcher(
        saleRepository = mock(SaleRepository::class.java),
        saleLineRepository = mock(SaleLineRepository::class.java),
        saleLineCogsRepository = saleLineCogsRepository,
        saleAssembler = mock(SaleAssembler::class.java),
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        saleLocker = mock(SaleLocker::class.java),
        saleVoidRepository = mock(SaleVoidRepository::class.java),
        userQualifier = mock(UserQualifier::class.java),
    )

    @Test
    fun `a sale with no active cost rows gets zero instead of going missing`() {
        val costedSaleId = UUID.randomUUID()
        val uncostedSaleId = UUID.randomUUID()
        val saleIds = listOf(costedSaleId, uncostedSaleId)
        `when`(saleLineCogsRepository.sumCogsTotalsBySaleIdsAndStatus(saleIds, SaleLineCogsStatus.ACTIVE))
            .thenReturn(listOf(object : SaleCogsTotalRow {
                override val saleId = costedSaleId
                override val cogsTotal = BigDecimal("12.5000")
            }))

        val cogsTotalBySaleId = saleDataFetcher.getSaleCogsTotals(saleIds)

        assertEquals(BigDecimal("12.5000"), cogsTotalBySaleId.getValue(costedSaleId))
        assertEquals(BigDecimal.ZERO, cogsTotalBySaleId.getValue(uncostedSaleId))
    }
}
