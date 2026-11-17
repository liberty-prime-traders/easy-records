package app.myliberty.easy_records.cross_tier.authority

import java.time.OffsetDateTime
import java.util.UUID

data class MembershipPeriod(
    val id: UUID,
    val since: OffsetDateTime,
    val until: OffsetDateTime?
)

data class UserAccessDetail(
    val userId: UUID,
    val memberships: List<MembershipPeriod>,
    val roles: List<GrantedRole>,
    val addOnPermissions: List<GrantedPermission>
)
