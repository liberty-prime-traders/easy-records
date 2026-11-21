package app.myliberty.easy_records.organizations.business.core.address

import java.time.OffsetDateTime
import java.util.UUID

data class AddressDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val line1: String? = null,
    val line2: String? = null,
    val line3: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)
