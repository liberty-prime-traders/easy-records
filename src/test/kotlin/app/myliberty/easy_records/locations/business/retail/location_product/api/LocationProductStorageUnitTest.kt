package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEnricher
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductForSaleAssembler
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductIdentityView
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductPagedSearch
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductRepository
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class LocationProductStorageUnitTest {

    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val gramId = UUID.randomUUID()
    private val miscPieceId = UUID.randomUUID()
    private val miscCartonId = UUID.randomUUID()
    private val locationProductRepository = mock(LocationProductRepository::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java).also {
        `when`(it.getOrLoad()).thenReturn(
            UnitConversionGraph(
                emptyMap(),
                emptyMap(),
                mapOf(
                    pieceId to UnitPlacement(pieceId, 1L),
                    baleId to UnitPlacement(pieceId, 18L),
                    gramId to UnitPlacement(gramId, 1L),
                    miscPieceId to UnitPlacement(miscPieceId, 1L),
                    miscCartonId to UnitPlacement(miscCartonId, 1L),
                )
            )
        )
    }
    private val fetcher = LocationProductDataFetcher(
        locationProductRepository,
        unitConversionGraphFacade,
        mock(LocationProductPagedSearch::class.java),
        mock(LocationProductEnricher::class.java),
        mock(LocationProductForSaleAssembler::class.java),
    )

    private fun productWithBaseUnit(baseUnitId: UUID): UUID {
        val locationProductId = UUID.randomUUID()
        val identityView = mock(LocationProductIdentityView::class.java).also {
            `when`(it.id).thenReturn(locationProductId)
            `when`(it.baseUnitId).thenReturn(baseUnitId)
        }
        `when`(locationProductRepository.findIdentityViewsByIdIn(listOf(locationProductId))).thenReturn(listOf(identityView))
        return locationProductId
    }

    @Test
    fun `a unit of the product's group captures its size in the storage unit`() {
        val locationProductId = productWithBaseUnit(baleId)

        val captured = fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, pieceId)))

        assertEquals(1L, captured.getValue(locationProductId))
        assertEquals(
            18L,
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, baleId))).getValue(locationProductId)
        )
    }

    @Test
    fun `a unit whose root differs from the product's storage unit is rejected`() {
        val locationProductId = productWithBaseUnit(baleId)

        val exception = assertThrows(RtsGenericException::class.java) {
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, gramId)))
        }

        assertEquals(LocationProductDataFetcher.UNIT_OUTSIDE_PRODUCT_GROUP, exception.message)
    }

    @Test
    fun `a miscellaneous product is limited to its own base unit with no special case`() {
        val locationProductId = productWithBaseUnit(miscPieceId)

        assertEquals(
            1L,
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, miscPieceId))).getValue(locationProductId)
        )
        assertThrows(RtsGenericException::class.java) {
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, miscCartonId)))
        }
    }
}
