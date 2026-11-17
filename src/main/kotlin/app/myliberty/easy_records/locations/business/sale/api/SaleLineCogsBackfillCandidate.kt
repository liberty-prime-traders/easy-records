package app.myliberty.easy_records.locations.business.sale.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleLineCogsBackfillCandidate(
    val saleLineId: UUID,
    val cogsTotal: BigDecimal,
    val dateSold: OffsetDateTime?,
)
