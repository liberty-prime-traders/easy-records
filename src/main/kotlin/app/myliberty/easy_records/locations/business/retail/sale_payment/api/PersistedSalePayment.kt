package app.myliberty.easy_records.locations.business.sale_payment.api

import java.time.OffsetDateTime
import java.util.UUID

data class PersistedSalePayment(
    val id: UUID,
    val paymentDate: OffsetDateTime,
)
