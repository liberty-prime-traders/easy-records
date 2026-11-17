package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine

data class SaleLineMergeResult(
    val mergedSaleLines: List<SaleSessionLine>,
    val mergedSaleAdjustments: List<SaleSessionAdjustment>,
    val problemSaleLines: List<SaleSessionLine>,
    val problemSaleAdjustments: List<SaleSessionAdjustment>,
    val orderLevelSaleAdjustments: List<SaleSessionAdjustment>,
    val problems: List<ConsolidationProblem>,
)
