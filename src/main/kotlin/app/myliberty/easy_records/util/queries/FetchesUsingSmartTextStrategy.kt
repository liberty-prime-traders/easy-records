package app.myliberty.easy_records.util.queries

import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse

interface FetchesUsingSmartTextStrategy<PARAMETERS, RESPONSE_TYPE> where PARAMETERS : HasSearchStrategy<PARAMETERS> {
    fun fetch(
        pageRequest: PageRequest<PARAMETERS, String>,
        setTimeout: Boolean
    ): PageResponse<RESPONSE_TYPE, String>
}
