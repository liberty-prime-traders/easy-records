package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentEntity
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentRepository
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockAdjustmentAccountCodesFetcherTest {

    private val stockAdjustmentRepository = mock(StockAdjustmentRepository::class.java)
    private val stockAdjustmentAccountCodesFetcher = StockAdjustmentAccountCodesFetcher(stockAdjustmentRepository)

    private fun adjustment(referenceNumber: String, category: StockAdjustmentCategory, direction: StockAdjustmentDirection) =
        StockAdjustmentEntity(
            locationProductId = UUID.randomUUID(), category = category, direction = direction,
            userProvidedQuantity = BigDecimal.ONE, unitId = UUID.randomUUID(), quantityText = "1 Piece",
            storageUnitsPerUserProvidedUnit = 1L,
            storageQuantity = BigDecimal.ONE, totalCost = BigDecimal("5.00"), stockMovementReasonId = null
        ).also { it.referenceNumber = referenceNumber }

    @Test
    fun `each adjustment maps to the accounts its category and direction post to`() {
        `when`(stockAdjustmentRepository.findByReferenceNumberIn(listOf("ADJ1", "ADJ2"))).thenReturn(
            listOf(
                adjustment("ADJ1", StockAdjustmentCategory.INTERNAL_USE, StockAdjustmentDirection.DECREASE),
                adjustment("ADJ2", StockAdjustmentCategory.OTHER, StockAdjustmentDirection.DECREASE),
            )
        )

        val accountCodesByReferenceNumber = stockAdjustmentAccountCodesFetcher.getByReferenceNumber(listOf("ADJ1", "ADJ2"))

        assertEquals(
            StockAdjustmentAccountCodes(SystemAccount.INTERNAL_USE_EXPENSE.code, SystemAccount.INVENTORY.code),
            accountCodesByReferenceNumber.getValue("ADJ1")
        )
        assertEquals(
            StockAdjustmentAccountCodes(SystemAccount.SHRINKAGE_AND_LOSSES.code, SystemAccount.INVENTORY.code),
            accountCodesByReferenceNumber.getValue("ADJ2")
        )
    }
}
