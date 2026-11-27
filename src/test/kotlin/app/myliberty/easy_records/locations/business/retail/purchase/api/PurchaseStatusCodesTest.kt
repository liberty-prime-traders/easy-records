package app.myliberty.easy_records.locations.business.retail.purchase.api

import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PurchaseStatusCodesTest {

  @Test
  fun `purchase status codes are the values stored in the database`() {
    val codesByStatusName = PurchaseStatus.entries.associate { it.name to it.code }

    assertEquals(
      mapOf(
        "DRAFT" to "DFT",
        "ORDERED" to "ORD",
        "PARTIALLY_DELIVERED" to "PDL",
        "FULLY_DELIVERED" to "FDL",
        "CANCELED" to "CLD"
      ),
      codesByStatusName
    )
  }

  @Test
  fun `delivery line status codes are the values stored in the database`() {
    val codesByStatusName = PurchaseDeliveryLineStatus.entries.associate { it.name to it.code }

    assertEquals(mapOf("ACTIVE" to "ACT", "REVISED" to "RVS"), codesByStatusName)
  }
}
