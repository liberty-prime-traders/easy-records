package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import java.math.BigDecimal
import java.util.UUID

data class ConsolidationCopy(
    val saleSession: SaleSession,
    val sourceSaleReferenceByLineKey: Map<UUID, String>,
    val sourceSaleLinesBySaleId: Map<UUID, List<SaleLineDto>>,
    val orderLevelAmountByAdjustmentKey: Map<UUID, BigDecimal>,
)
