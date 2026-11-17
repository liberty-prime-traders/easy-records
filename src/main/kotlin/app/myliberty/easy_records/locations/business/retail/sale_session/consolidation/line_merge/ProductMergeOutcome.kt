package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine

sealed interface ProductMergeOutcome {

    data class Merged(
        val saleLine: SaleSessionLine,
        val saleAdjustments: List<SaleSessionAdjustment>,
    ) : ProductMergeOutcome

    data class Blocked(val kind: ConsolidationProblemKind) : ProductMergeOutcome
}
