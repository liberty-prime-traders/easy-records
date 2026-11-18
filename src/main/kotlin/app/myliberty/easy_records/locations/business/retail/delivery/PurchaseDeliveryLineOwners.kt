package app.myliberty.easy_records.locations.business.retail.delivery

import java.util.UUID

interface PurchaseDeliveryLineOwners {
  val purchaseId: UUID
  val purchaseLineId: UUID
}
