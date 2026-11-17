package app.myliberty.easy_records.locations.business.retail.reports.payments

import app.myliberty.easy_records.locations.business.retail.reports.payments.api.PaymentType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentRow(
  val id: UUID,
  val type: PaymentType,
  val parentReferenceNumber: String,
  val contactId: UUID,
  val paymentMethodId: UUID,
  val amount: BigDecimal,
  val paymentDate: OffsetDateTime,
  val recordedOn: OffsetDateTime,
  val voidReason: String?
)
