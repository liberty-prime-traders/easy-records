package app.myliberty.easy_records.platform.business.cache_admin.api

data class CacheClearResponseDto(
    val organizationsCleared: Int,
    val schemasCleared: Int,
    val entriesEvicted: Int
)
