package app.myliberty.easy_records.cross_tier.authority

import app.myliberty.easy_records.util.enums.SchemaLevel

enum class AuthorityType { ROLE, PERMISSION }

data class Authority(
    val name: String,
    val tier: SchemaLevel,
    val type: AuthorityType,
    val holderCount: Int?
)
