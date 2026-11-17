package app.myliberty.easy_records.locations.business.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class DeliveryLineRevisionTotalsDto(
    val purchaseId: UUID,
    val revisedLineTotal: BigDecimal,
    val replacementLineTotal: BigDecimal,
)
