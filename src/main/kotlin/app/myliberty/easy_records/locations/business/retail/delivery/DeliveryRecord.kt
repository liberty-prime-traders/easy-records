package app.myliberty.easy_records.locations.business.delivery

data class DeliveryRecord(
  val delivery: PurchaseDeliveryEntity,
  val lines: List<PurchaseDeliveryLineEntity>
)
