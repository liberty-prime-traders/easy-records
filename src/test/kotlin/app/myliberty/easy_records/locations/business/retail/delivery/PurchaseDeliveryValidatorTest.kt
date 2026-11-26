package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryCreateDto
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryLineCreateDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryValidatorTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val pieceBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pc", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))),
        unitId
    )
    private val purchaseDeliveryValidator = PurchaseDeliveryValidator(
        mock(LocationProductDataFetcher::class.java).also {
            val summary = mock(LocationProductSummaryDto::class.java)
            `when`(summary.label).thenReturn("Sugar")
            `when`(it.findSummaryByIds(setOf(locationProductId))).thenReturn(mapOf(locationProductId to summary))
        },
        mock(ProductStockBreakdownProvider::class.java).also {
            `when`(it.forLocationProduct(locationProductId)).thenReturn(pieceBreakdown)
            `when`(it.forLocationProducts(setOf(locationProductId)))
                .thenReturn(mapOf(locationProductId to pieceBreakdown))
        }
    )

    private val purchaseLineId = UUID.randomUUID()
    private val purchaseLineById = mapOf(
        purchaseLineId to PurchaseLineDto(
            id = purchaseLineId,
            purchaseId = UUID.randomUUID(),
            locationProductId = locationProductId,
            unitCost = BigDecimal("5.00"),
            storageUnitsPerUserProvidedUnit = 1L,
            unitId = unitId,
            storageNetOrderedQty = ExactQuantity.ofStorage(BigDecimal("10")),
            storagePendingDeliveryQty = ExactQuantity.ofStorage(BigDecimal("6"))
        )
    )

    private fun deliveryOf(vararg purchaseLineIds: UUID, quantity: String = "1") = PurchaseDeliveryCreateDto(
        purchaseId = UUID.randomUUID(),
        deliveredAt = OffsetDateTime.parse("2026-11-01T10:00:00Z"),
        notes = null,
        lines = purchaseLineIds.map {
            PurchaseDeliveryLineCreateDto(
                purchaseLineId = it,
                userProvidedQuantityDelivered = PositiveUserProvidedQuantity.of(BigDecimal(quantity)),
                unitId = unitId,
                unitCost = PositiveMoney.of(BigDecimal("5.00"))
            )
        }
    )

    @Test
    fun `a delivery without lines is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDeliveryValidator.validate(deliveryOf(), purchaseLineById)
        }

        assertEquals("Delivery must have at least one line.", exception.message)
    }

    @Test
    fun `the same purchase line twice in one delivery is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDeliveryValidator.validate(deliveryOf(purchaseLineId, purchaseLineId), purchaseLineById)
        }

        assertEquals("Duplicate purchase line IDs in delivery request.", exception.message)
    }

    @Test
    fun `a line that belongs to another purchase is rejected`() {
        val foreignLineId = UUID.randomUUID()

        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDeliveryValidator.validate(deliveryOf(foreignLineId), purchaseLineById)
        }

        assertEquals("Purchase line $foreignLineId does not belong to this purchase.", exception.message)
    }

    @Test
    fun `lines of the purchase pass validation`() {
        purchaseDeliveryValidator.validate(deliveryOf(purchaseLineId), purchaseLineById)
    }

    @Test
    fun `delivering exactly the pending quantity is allowed`() {
        purchaseDeliveryValidator.ensurePendingDeliveryQuantityIsNotExceeded(
            deliveryOf(purchaseLineId, quantity = "6"), purchaseLineById, mapOf(purchaseLineId to 1L)
        )
    }

    @Test
    fun `delivering one storage unit above the pending quantity is rejected with both quantities`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDeliveryValidator.ensurePendingDeliveryQuantityIsNotExceeded(
                deliveryOf(purchaseLineId, quantity = "7"), purchaseLineById, mapOf(purchaseLineId to 1L)
            )
        }

        assertEquals("You can deliver at most 6 pcs of 'Sugar', but you entered 7 pcs.", exception.message)
    }

    @Test
    fun `the typed quantity is multiplied by the captured storage ratio before the comparison`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            purchaseDeliveryValidator.ensurePendingDeliveryQuantityIsNotExceeded(
                deliveryOf(purchaseLineId, quantity = "1"), purchaseLineById, mapOf(purchaseLineId to 7L)
            )
        }

        assertEquals("You can deliver at most 6 pcs of 'Sugar', but you entered 7 pcs.", exception.message)
    }

    @Test
    fun `a quantity within the maximum has no problem`() {
        assertNull(
            purchaseDeliveryValidator.maximumQuantityProblem(
                "DELN000001", locationProductId,
                ExactQuantity.ofStorage(BigDecimal("6")), ExactQuantity.ofStorage(BigDecimal("6"))
            )
        )
    }

    @Test
    fun `a quantity above the maximum says what is allowed and what to enter`() {
        val problem = purchaseDeliveryValidator.maximumQuantityProblem(
            "DELN000001", locationProductId,
            ExactQuantity.ofStorage(BigDecimal("6")), ExactQuantity.ofStorage(BigDecimal("7"))
        )!!

        assertEquals("DELN000001", problem.documentReference)
        assertEquals("You can deliver at most 6 pcs of 'Sugar', but you entered 7 pcs.", problem.description)
        assertEquals("Enter 6 pcs or less.", problem.effect)
    }
}
