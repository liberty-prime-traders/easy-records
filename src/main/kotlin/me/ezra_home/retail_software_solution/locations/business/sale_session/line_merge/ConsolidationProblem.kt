package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import java.util.UUID

data class ConsolidationProblem(
    val locationProductId: UUID,
    val kind: ConsolidationProblemKind,
    val saleLineIdentities: List<SessionIdentity>,
)
