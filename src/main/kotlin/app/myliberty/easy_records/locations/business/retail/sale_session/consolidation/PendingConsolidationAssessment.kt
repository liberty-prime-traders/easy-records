package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.sale_session.api.ConsolidationIssueResponse

data class PendingConsolidationAssessment(
    val consolidationIssues: List<ConsolidationIssueResponse>,
) {

    val canGenerateConsolidatedSale: Boolean = consolidationIssues.none { it.blocking }
}
