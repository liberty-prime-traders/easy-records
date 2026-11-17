package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine

sealed interface ProductMergeOutcome {

    data class Merged(
        val saleLine: SaleSessionLine,
        val saleAdjustments: List<SaleSessionAdjustment>,
    ) : ProductMergeOutcome

    data class Blocked(val kind: ConsolidationProblemKind) : ProductMergeOutcome
}
