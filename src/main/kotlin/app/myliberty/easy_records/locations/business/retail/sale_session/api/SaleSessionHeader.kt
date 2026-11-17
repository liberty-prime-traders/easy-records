package app.myliberty.easy_records.locations.business.sale_session.api

import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionHeader(
    val referenceNumber: String?,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
)
