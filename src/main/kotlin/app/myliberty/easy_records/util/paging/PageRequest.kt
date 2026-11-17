package app.myliberty.easy_records.util.paging

data class PageRequest<PARAMETER, CURSOR>  (
    val previousCursor: CURSOR,
    val requestedSize: Int,
    val parameters: PARAMETER
)
