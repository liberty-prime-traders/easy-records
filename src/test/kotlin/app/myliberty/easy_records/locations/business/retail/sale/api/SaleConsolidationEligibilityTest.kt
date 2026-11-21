package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.util.enums.SystemContact
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class SaleConsolidationEligibilityTest {

    private val namedCustomerId = UUID.randomUUID()

    @Test
    fun `only draft and confirmed sales of a named customer can be consolidated`() {
        SaleStatus.entries.forEach { saleStatus ->
            val expected = saleStatus == SaleStatus.DRAFT || saleStatus == SaleStatus.CONFIRMED
            assertEquals(expected, SaleConsolidationEligibility.canConsolidate(saleStatus, namedCustomerId), saleStatus.name)
        }
    }

    @Test
    fun `a walk-in sale cannot be consolidated whatever its status`() {
        SaleStatus.entries.forEach { saleStatus ->
            assertEquals(
                false,
                SaleConsolidationEligibility.canConsolidate(saleStatus, SystemContact.WALK_IN.id),
                saleStatus.name
            )
        }
    }
}
