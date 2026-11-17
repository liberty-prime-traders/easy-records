package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

data class ConsolidationSourceSale(
    val saleId: UUID,
    val saleVersion: Long,
    val referenceNumber: String,
)
