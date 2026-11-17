package app.myliberty.easy_records.cross_tier.core.authority

import java.util.UUID

data class MembershipUserSummary(
    val userId: UUID,
    val fullName: String?,
    val email: String?,
    val membershipCount: Long,
    val active: Boolean
)
