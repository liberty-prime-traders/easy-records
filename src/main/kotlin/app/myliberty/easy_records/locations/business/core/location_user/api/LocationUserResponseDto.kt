package app.myliberty.easy_records.locations.business.core.location_user.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class LocationUserResponseDto(
    val id: UUID,
    val user: String?,
    val userId: UUID,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
