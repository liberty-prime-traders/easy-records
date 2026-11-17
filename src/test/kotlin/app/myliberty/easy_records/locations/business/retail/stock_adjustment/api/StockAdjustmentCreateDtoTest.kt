package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class StockAdjustmentCreateDtoTest {

    @Test
    fun `the required unit cost is the unit cost when it was supplied`() {
        val unitCost = PositiveMoney.of(BigDecimal("2.50"))

        assertEquals(unitCost, stockAdjustmentCreateDto(unitCost).requiredUnitCost())
    }

    @Test
    fun `a missing unit cost is a business error naming the rule`() {
        val exception = assertThrows(RtsGenericException::class.java) { stockAdjustmentCreateDto(null).requiredUnitCost() }

        assertEquals("Unit cost is required for a stock increase", exception.message)
    }

    private fun stockAdjustmentCreateDto(unitCost: PositiveMoney?) = StockAdjustmentCreateDto(
        locationProductId = UUID.randomUUID(),
        category = StockAdjustmentCategory.FREE_GOODS,
        userProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal.ONE),
        unitId = UUID.randomUUID(),
        note = "samples",
        unitCost = unitCost
    )
}
