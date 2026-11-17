package app.myliberty.easy_records.locations.business.retail.sale.api

import java.util.UUID

data class SaleHeaderDto(
    val id: UUID,
    val referenceNumber: String,
    val version: Long,
    val status: SaleStatus,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: java.time.OffsetDateTime?,
    val notes: String?,
)
