package app.myliberty.easy_records.platform.business.db_version.api

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import app.myliberty.easy_records.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class DbVersionResponseDto(
    val id: UUID,
    val versionNumber: String?,
    val sequenceNumber: Long?,
    val prevVersion: String?,
    @field:JsonSerialize(using = DatesToMillis::class)
    val activatedOn: OffsetDateTime?
): Serializable
