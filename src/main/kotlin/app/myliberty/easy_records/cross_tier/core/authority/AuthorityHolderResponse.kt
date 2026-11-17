package app.myliberty.easy_records.cross_tier.authority

import java.time.OffsetDateTime

data class AuthorityHolderResponse(
    val fullName: String?,
    val grantedOn: OffsetDateTime,
    val authorityName: String
)
