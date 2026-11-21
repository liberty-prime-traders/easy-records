package app.myliberty.easy_records.locations.business.retail.sale_session.api

data class ConsolidationIssueResponse(
    val saleReferences: String,
    val description: String,
    val fix: String,
    val blocking: Boolean = true,
)
