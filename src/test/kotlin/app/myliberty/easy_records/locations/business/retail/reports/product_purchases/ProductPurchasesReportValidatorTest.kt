package app.myliberty.easy_records.locations.business.retail.reports.product_purchases

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class ProductPurchasesReportValidatorTest {

  private val from = OffsetDateTime.parse("2026-10-01T00:00:00+00:00")
  private val before = OffsetDateTime.parse("2026-10-08T00:00:00+00:00")

  @Test
  fun `resolves a bounded range`() {
    val range = ProductPurchasesReportValidator.resolveDeliveredRange(from, before)

    assertEquals(from, range.fromInclusive)
    assertEquals(before, range.beforeExclusive)
  }

  @Test
  fun `rejects a missing bound`() {
    assertThrows(LibertyGenericException::class.java) { ProductPurchasesReportValidator.resolveDeliveredRange(null, before) }
    assertThrows(LibertyGenericException::class.java) { ProductPurchasesReportValidator.resolveDeliveredRange(from, null) }
  }

  @Test
  fun `rejects a range that runs backwards`() {
    assertThrows(LibertyGenericException::class.java) { ProductPurchasesReportValidator.resolveDeliveredRange(before, from) }
  }

  @Test
  fun `rejects a range longer than one year`() {
    assertThrows(LibertyGenericException::class.java) {
      ProductPurchasesReportValidator.resolveDeliveredRange(from, from.plusYears(2))
    }
  }

  @Test
  fun `accepts page sizes at both bounds and rejects values outside them`() {
    ProductPurchasesReportValidator.guardValidPageSize(1)
    ProductPurchasesReportValidator.guardValidPageSize(500)
    assertThrows(LibertyGenericException::class.java) { ProductPurchasesReportValidator.guardValidPageSize(0) }
    assertThrows(LibertyGenericException::class.java) { ProductPurchasesReportValidator.guardValidPageSize(501) }
  }
}
