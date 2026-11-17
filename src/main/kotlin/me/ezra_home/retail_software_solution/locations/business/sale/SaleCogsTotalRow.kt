package me.ezra_home.retail_software_solution.locations.business.sale

import java.math.BigDecimal
import java.util.UUID

interface SaleCogsTotalRow {
    val saleId: UUID
    val cogsTotal: BigDecimal
}
