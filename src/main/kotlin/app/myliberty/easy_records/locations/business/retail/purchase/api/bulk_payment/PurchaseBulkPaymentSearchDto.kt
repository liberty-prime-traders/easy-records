package app.myliberty.easy_records.locations.business.purchase.api.bulk_payment

import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseBulkPaymentSearchDto(
  val supplierId: UUID,
  val orderedFrom: OffsetDateTime,
  val orderedBefore: OffsetDateTime
)
