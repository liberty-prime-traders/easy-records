package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine

data class SaleLineMergeResult(
    val mergedSaleLines: List<SaleSessionLine>,
    val mergedSaleAdjustments: List<SaleSessionAdjustment>,
    val problemSaleLines: List<SaleSessionLine>,
    val problemSaleAdjustments: List<SaleSessionAdjustment>,
    val orderLevelSaleAdjustments: List<SaleSessionAdjustment>,
    val problems: List<ConsolidationProblem>,
)
