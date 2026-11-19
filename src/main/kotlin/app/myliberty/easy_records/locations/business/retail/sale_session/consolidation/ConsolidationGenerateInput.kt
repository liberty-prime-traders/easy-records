package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import java.util.UUID

data class ConsolidationGenerateInput(
    val sourceSales: List<ConsolidationSourceSale>,
    val reviewedProductLines: ReviewedProductLines,
    val pendingSessionId: UUID?,
)
