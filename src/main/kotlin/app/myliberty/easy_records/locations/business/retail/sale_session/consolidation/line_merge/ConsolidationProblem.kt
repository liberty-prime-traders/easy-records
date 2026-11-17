package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.api.SessionIdentity
import java.util.UUID

data class ConsolidationProblem(
    val locationProductId: UUID,
    val kind: ConsolidationProblemKind,
    val saleLineIdentities: List<SessionIdentity>,
)
