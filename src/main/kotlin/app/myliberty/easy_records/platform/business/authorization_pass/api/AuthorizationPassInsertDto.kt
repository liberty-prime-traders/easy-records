package app.myliberty.easy_records.platform.business.authorization_pass.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class AuthorizationPassInsertDto(
    val passType: PassType,
    val maxUseCount: Int,
    val assignedToId: UUID,
    val expiresOn: OffsetDateTime? = null
) : Serializable
