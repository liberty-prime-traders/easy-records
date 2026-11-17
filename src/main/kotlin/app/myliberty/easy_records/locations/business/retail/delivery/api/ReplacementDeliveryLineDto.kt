package app.myliberty.easy_records.locations.business.retail.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class ReplacementDeliveryLineDto(
    val deliveryReferenceNumber: String,
    val revisedLineReferenceNumber: String,
    val replacementLineReferenceNumber: String,
    val purchaseLineId: UUID,
    val userProvidedQuantityDelivered: BigDecimal,
    val unitCost: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val revisedLineTotal: BigDecimal,
    val replacementLineTotal: BigDecimal,
)
