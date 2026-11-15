package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ConsolidationIssueResponse

data class PendingConsolidationAssessment(
    val consolidationIssues: List<ConsolidationIssueResponse>,
) {

    val canGenerateConsolidatedSale: Boolean = consolidationIssues.isEmpty()
}
