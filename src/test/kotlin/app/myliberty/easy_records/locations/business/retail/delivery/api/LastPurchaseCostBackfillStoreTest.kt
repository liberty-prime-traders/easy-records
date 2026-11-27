package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.locations.business.retail.delivery.LastDeliveredCostProjection
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.retail.location_product.api.LastPurchaseCostUpdateDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class LastPurchaseCostBackfillStoreTest {

    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val lastPurchaseCostBackfillStore = LastPurchaseCostBackfillStore(purchaseDeliveryLineRepository)

    @Test
    fun `received active delivery costs after the cursor become last purchase cost updates`() {
        val afterLocationProductId = UUID.randomUUID()
        val locationProductId = UUID.randomUUID()
        val projection = mock(LastDeliveredCostProjection::class.java)
        `when`(projection.locationProductId).thenReturn(locationProductId)
        `when`(projection.unitCost).thenReturn(BigDecimal("60.00"))
        `when`(projection.storageUnitsPerUserProvidedUnit).thenReturn(12L)
        `when`(purchaseDeliveryLineRepository.findLastDeliveredCostsAfter("R", "ACT", afterLocationProductId, 50))
            .thenReturn(listOf(projection))

        val updates = lastPurchaseCostBackfillStore.findLastDeliveredCostsAfter(afterLocationProductId, 50)

        assertEquals(listOf(LastPurchaseCostUpdateDto(locationProductId, BigDecimal("60.00"), 12L)), updates)
    }
}
