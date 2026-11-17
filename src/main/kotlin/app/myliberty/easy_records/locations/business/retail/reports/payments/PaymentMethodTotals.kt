package app.myliberty.easy_records.locations.business.retail.reports.payments

import java.math.BigDecimal
import java.util.UUID

data class PaymentMethodTotals(
  val paymentMethodId: UUID,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal,
  val activeCount: Long,
  val voidedCount: Long
)
