package app.myliberty.easy_records.cross_tier.authority

import java.util.UUID

data class MembershipSummaryRow(
    val userId: UUID,
    val membershipCount: Long,
    val active: Boolean
)
