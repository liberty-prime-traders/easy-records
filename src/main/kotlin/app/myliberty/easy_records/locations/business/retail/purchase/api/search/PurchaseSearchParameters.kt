package app.myliberty.easy_records.locations.business.purchase.api.search

import app.myliberty.easy_records.locations.business.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseSearchParameters(
  val recordedFrom: OffsetDateTime? = null,
  val recordedBefore: OffsetDateTime? = null,
  val purchaseDateFrom: OffsetDateTime? = null,
  val purchaseDateBefore: OffsetDateTime? = null,
  val supplierIds: List<UUID> = emptyList(),
  val purchaseStatuses: List<PurchaseStatus> = emptyList(),
  val paymentStatuses: List<PaymentStatus> = emptyList(),
  val minAmount: BigDecimal? = null,
  val maxAmount: BigDecimal? = null,
  val purchaseReferenceNumbers: List<String> = emptyList()
) {

  fun sanitized(): PurchaseSearchParameters = copy(purchaseReferenceNumbers = StringUtils.dropBlank(purchaseReferenceNumbers))
}
