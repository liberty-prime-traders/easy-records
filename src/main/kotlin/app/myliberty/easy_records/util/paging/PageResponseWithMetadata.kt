package app.myliberty.easy_records.util.paging

data class PageResponseWithMetadata<CONTENT, CURSOR, METADATA>(
    val currentCursor: CURSOR,
    val hasMore: Boolean,
    val contents: Collection<CONTENT>,
    val metadata: METADATA,
    var requireClientSideFilter: Boolean = false
)
