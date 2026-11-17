package app.myliberty.easy_records.locations.business.retail.sale_session.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionPayment(
    val identity: SessionIdentity,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String? = null,
)
