package app.myliberty.easy_records.platform.business.authorization_pass.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class AuthorizationPassUpdateDto(
    val id: UUID,
    val maxUseCount: Optional<Int>? = null,
    val expiresOn: Optional<OffsetDateTime>? = null
) : Serializable
