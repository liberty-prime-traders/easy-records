package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ConsolidationSourceSale
import java.util.UUID

data class ConsolidationGenerateInput(
    val sourceSales: List<ConsolidationSourceSale>,
    val reviewedProductLines: ReviewedProductLines,
    val pendingSessionId: UUID?,
)
