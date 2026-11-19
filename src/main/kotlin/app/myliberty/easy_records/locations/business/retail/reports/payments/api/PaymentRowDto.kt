package app.myliberty.easy_records.locations.business.retail.reports.payments.api

import app.myliberty.easy_records.util.ui_models.BalanceEffect
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentRowDto(
  val id: UUID,
  val type: PaymentType,
  val parentReferenceNumber: String,
  val contactName: String?,
  val paymentMethodName: String,
  val amount: BigDecimal,
  val balanceEffect: BalanceEffect,
  val paymentDate: OffsetDateTime,
  val recordedOn: OffsetDateTime,
  val voided: Boolean,
  val voidedReason: String?
)
