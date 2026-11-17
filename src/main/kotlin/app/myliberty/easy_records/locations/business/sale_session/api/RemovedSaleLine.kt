package me.ezra_home.retail_software_solution.locations.business.sale_session.api

data class RemovedSaleLine(
    val saleLine: SaleSessionLine,
    val saleAdjustments: List<SaleSessionAdjustment>,
)
