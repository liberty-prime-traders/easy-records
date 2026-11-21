package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductCache
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEnricher
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEntity
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductMapper
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductRepository
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductService
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class LocationProductServiceLastPurchaseCostTest {

    private val storageUnitId = UUID.randomUUID()
    private val dozenUnitId = UUID.randomUUID()
    private val unknownUnitId = UUID.randomUUID()
    private val savedProducts = mutableListOf<LocationProductEntity>()
    private val products = mutableListOf<LocationProductEntity>()
    private val locationProductRepository = mock(LocationProductRepository::class.java) { invocation ->
        when (invocation.method.name) {
            "findAllById" -> products.toList()
            "saveAll" -> invocation.getArgument<Iterable<LocationProductEntity>>(0).toList()
                .also { savedProducts.addAll(it) }
            else -> null
        }
    }
    private val locationProductCache = mock(LocationProductCache::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
    private val locationProductService = LocationProductService(
        locationProductRepository,
        locationProductCache,
        mock(LocationProductMapper::class.java),
        mock(LocationProductEnricher::class.java),
        mock(OrganizationProductService::class.java),
        unitConversionGraphFacade,
    )

    init {
        `when`(unitConversionGraphFacade.getOrLoad()).thenReturn(
            UnitConversionGraph(
                fromUnitToTargets = emptyMap(),
                placementByUnitId = mapOf(dozenUnitId to UnitPlacement(storageUnitId, 12)),
            )
        )
    }

    @Test
    fun `the later line of a product that appears twice sets the cost per base unit`() {
        val product = product(dozenUnitId)
        val earlierLine = LastPurchaseCostUpdateDto(product.id!!, BigDecimal("5.00"), 1)
        val laterLine = LastPurchaseCostUpdateDto(product.id!!, BigDecimal("100.00"), 12)

        locationProductService.updateLastPurchaseCosts(listOf(earlierLine, laterLine))

        assertEquals(BigDecimal("100.0000"), product.lastPurchasePrice)
        assertEquals(listOf(product), savedProducts)
        verify(locationProductCache).evictAll()
    }

    @Test
    fun `a cost typed per piece is stored per base unit`() {
        val product = product(dozenUnitId)

        locationProductService.updateLastPurchaseCosts(
            listOf(LastPurchaseCostUpdateDto(product.id!!, BigDecimal("5.00"), 1))
        )

        assertEquals(BigDecimal("60.0000"), product.lastPurchasePrice)
    }

    @Test
    fun `a product whose base unit is unknown fails the whole update and saves nothing`() {
        val product = product(unknownUnitId)

        assertThrows(LibertyGenericException::class.java) {
            locationProductService.updateLastPurchaseCosts(
                listOf(LastPurchaseCostUpdateDto(product.id!!, BigDecimal("5.00"), 1))
            )
        }

        assertTrue(savedProducts.isEmpty())
        verify(locationProductCache, never()).evictAll()
    }

    private fun product(baseUnitId: UUID) = LocationProductEntity(
        orgProductId = UUID.randomUUID(),
        productName = "Sugar",
        productGroupName = "Staples",
        categoryId = UUID.randomUUID(),
        baseUnitId = baseUnitId,
    ).also {
        it.id = UUID.randomUUID()
        products.add(it)
    }
}
