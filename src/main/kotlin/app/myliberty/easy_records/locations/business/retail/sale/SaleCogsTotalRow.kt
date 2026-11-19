package app.myliberty.easy_records.locations.business.retail.sale

import java.math.BigDecimal
import java.util.UUID

interface SaleCogsTotalRow {
    val saleId: UUID
    val cogsTotal: BigDecimal
}
