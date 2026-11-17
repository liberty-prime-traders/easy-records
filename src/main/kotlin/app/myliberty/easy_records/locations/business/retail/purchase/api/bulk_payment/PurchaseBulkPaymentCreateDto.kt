package app.myliberty.easy_records.locations.business.retail.purchase.api.bulk_payment

import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseBulkPaymentCreateDto(
  val supplierId: UUID,
  val purchaseIds: Set<UUID>,
  val amount: PositiveMoney,
  val paymentMethodId: UUID,
  val paymentDate: OffsetDateTime,
  val notes: String?
)
