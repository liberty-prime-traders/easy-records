package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class StockAdjustmentSearchValidatorTest {

    private val adjustedFrom = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val adjustedBefore = OffsetDateTime.parse("2026-02-01T00:00:00Z")

    @Test
    fun `a bounded range with no other filters is accepted`() {
        StockAdjustmentSearchValidator.guardValidParameters(
            StockAdjustmentSearchParameters(adjustedFrom = adjustedFrom, adjustedBefore = adjustedBefore)
        )
    }

    @Test
    fun `a missing adjustedFrom or adjustedBefore is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(adjustedBefore = adjustedBefore)
            )
        }
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(adjustedFrom = adjustedFrom)
            )
        }
    }

    @Test
    fun `a range wider than one year is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(
                    adjustedFrom = adjustedFrom,
                    adjustedBefore = adjustedFrom.plusYears(1).plusDays(1)
                )
            )
        }
    }

    @Test
    fun `adjustedFrom after adjustedBefore is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(adjustedFrom = adjustedBefore, adjustedBefore = adjustedFrom)
            )
        }
    }

    @Test
    fun `more than 100 product ids or adjusted by ids are rejected`() {
        val tooManyIds = List(101) { UUID.randomUUID() }
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(adjustedFrom, adjustedBefore, locationProductIds = tooManyIds)
            )
        }
        assertThrows(LibertyGenericException::class.java) {
            StockAdjustmentSearchValidator.guardValidParameters(
                StockAdjustmentSearchParameters(adjustedFrom, adjustedBefore, adjustedByIds = tooManyIds)
            )
        }
    }
}
