package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

data class PendingCleanProduct(
    val locationProductId: UUID,
    val productLabel: String,
    val sourceSaleReferences: List<String>,
)
