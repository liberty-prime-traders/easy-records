package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductCache
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEntity
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductMapper
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductRepository
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

class LocationProductSyncServiceTest {

    private val orgProductId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val locationProductRepository = mock(LocationProductRepository::class.java)
    private val locationProductCache = mock(LocationProductCache::class.java)
    private val syncService = LocationProductSyncService(locationProductRepository, locationProductCache, mock(LocationProductMapper::class.java))

    private val existing = LocationProductEntity(
        orgProductId = orgProductId,
        productName = "Bella",
        productGroupName = "Rice",
        categoryId = categoryId,
        baseUnitId = baleId,
    )

    private fun syncDto(baseUnitId: UUID) = LocationProductSyncDto(
        orgProductId = orgProductId,
        productName = "Bella",
        description = null,
        productGroupName = "Rice",
        status = ProductStatus.ACTIVE,
        baseUnitId = baseUnitId,
        categoryId = categoryId,
    )

    @Test
    fun `a sync carrying a different base unit for an existing location product is rejected and nothing is saved`() {
        `when`(locationProductRepository.findByOrgProductId(orgProductId)).thenReturn(existing)

        val exception = assertThrows(RtsGenericException::class.java) { syncService.syncUpsert(syncDto(UUID.randomUUID())) }

        assertEquals(LocationProductSyncService.BASE_UNIT_CANNOT_CHANGE, exception.message)
        assertEquals(baleId, existing.baseUnitId)
        verifyNoInteractions(locationProductCache)
    }

    @Test
    fun `a sync with the same base unit and nothing else changed is a no-op`() {
        `when`(locationProductRepository.findByOrgProductId(orgProductId)).thenReturn(existing)

        assertEquals(false, syncService.syncUpsert(syncDto(baleId)))
    }
}
