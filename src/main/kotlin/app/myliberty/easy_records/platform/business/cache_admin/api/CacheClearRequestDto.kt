package app.myliberty.easy_records.platform.business.cache_admin.api

import java.util.UUID

data class CacheClearRequestDto(
    val organizationIds: Set<UUID>? = null
)
