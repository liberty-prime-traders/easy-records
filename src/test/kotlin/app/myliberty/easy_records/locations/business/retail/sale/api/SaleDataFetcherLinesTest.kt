package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.sale.SaleAssembler
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineCogsRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineEntity
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleLocker
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleDataFetcherLinesTest {

    private val saleLineRepository = mock(SaleLineRepository::class.java)
    private val firstProductId = UUID.randomUUID()
    private val secondProductId = UUID.randomUUID()
    private val firstBaseUnitId = UUID.randomUUID()
    private val secondBaseUnitId = UUID.randomUUID()
    private val productSummaries = mapOf(
        firstProductId to productSummary(firstProductId, firstBaseUnitId),
        secondProductId to productSummary(secondProductId, secondBaseUnitId),
    )
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java) { invocation ->
        if (invocation.method.name == "findSummaryByIds") productSummaries else null
    }
    private val saleDataFetcher = SaleDataFetcher(
        mock(SaleRepository::class.java),
        saleLineRepository,
        mock(SaleLineCogsRepository::class.java),
        mock(SaleAssembler::class.java),
        locationProductDataFetcher,
        mock(SaleLocker::class.java),
        mock(SaleVoidRepository::class.java),
        mock(UserQualifier::class.java),
    )

    @Test
    fun `lines are grouped by sale in query order, with base units and an empty entry for a sale without lines`() {
        val firstSaleId = UUID.randomUUID()
        val secondSaleId = UUID.randomUUID()
        val saleIdWithoutLines = UUID.randomUUID()
        val firstSaleFirstLine = lineEntity(firstSaleId, firstProductId)
        val secondSaleLine = lineEntity(secondSaleId, firstProductId)
        val firstSaleSecondLine = lineEntity(firstSaleId, secondProductId)
        val saleIds = listOf(firstSaleId, secondSaleId, saleIdWithoutLines)
        `when`(saleLineRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(saleIds))
            .thenReturn(listOf(firstSaleFirstLine, secondSaleLine, firstSaleSecondLine))

        val linesBySaleId = saleDataFetcher.getSaleLinesBySaleId(saleIds)

        assertEquals(saleIds, linesBySaleId.keys.toList())
        assertEquals(
            listOf(firstSaleFirstLine.id, firstSaleSecondLine.id),
            linesBySaleId.getValue(firstSaleId).map { it.id },
        )
        assertEquals(
            listOf(firstBaseUnitId, secondBaseUnitId), linesBySaleId.getValue(firstSaleId).map { it.baseUnitId }
        )
        assertEquals(listOf(secondSaleLine.id), linesBySaleId.getValue(secondSaleId).map { it.id })
        assertEquals(emptyList<SaleLineDto>(), linesBySaleId.getValue(saleIdWithoutLines))
    }

    @Test
    fun `the single-sale fetch is the one-id case of the batch`() {
        val saleId = UUID.randomUUID()
        val line = lineEntity(saleId, firstProductId)
        `when`(saleLineRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(listOf(saleId))).thenReturn(listOf(line))

        val lines = saleDataFetcher.getSaleLines(saleId)

        assertEquals(listOf(line.id), lines.map { it.id })
    }

    private fun lineEntity(saleId: UUID, locationProductId: UUID) = SaleLineEntity(
        saleId = saleId,
        locationProductId = locationProductId,
        userProvidedQuantity = BigDecimal("2"),
        unitId = UUID.randomUUID(),
        lineTotal = BigDecimal("30.00"),
        defaultSalePrice = BigDecimal("15.00"),
        storageUnitsPerUserProvidedUnit = 1,
    ).also { it.id = UUID.randomUUID() }

    private fun productSummary(locationProductId: UUID, baseUnitId: UUID) = LocationProductSummaryDto(
        id = locationProductId,
        referenceNumber = "LP-1",
        orgProductId = UUID.randomUUID(),
        productName = "Sugar",
        productGroupName = "Staples",
        baseUnitId = baseUnitId,
    )
}
