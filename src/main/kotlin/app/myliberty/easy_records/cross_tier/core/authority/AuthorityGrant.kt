package app.myliberty.easy_records.cross_tier.authority

import java.time.OffsetDateTime
import java.util.UUID

data class AuthorityGrant(
    val userId: UUID,
    val grantedAt: OffsetDateTime
)
