package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class ValidatedDeliveryLineRevisionDto(
    val revisedDeliveryLineId: UUID,
    val deliveryReferenceNumber: String,
    val revisedLineReferenceNumber: String,
    val purchaseLineId: UUID,
    val locationProductId: UUID,
    val revisedUnitId: UUID,
    val revisedStorageUnitsPerUserProvidedUnit: Long,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val revisedUserProvidedQuantityDelivered: BigDecimal,
    val revisedUnitCost: BigDecimal,
    val userProvidedQuantityDelivered: BigDecimal,
    val unitCost: BigDecimal,
    val problems: List<BlockingProblem>,
) {
    val replacementStorageQuantity: ExactQuantity =
        ExactQuantity.ofStorageFromUserProvided(userProvidedQuantityDelivered, storageUnitsPerUserProvidedUnit)
    val storageQuantityChange: ExactQuantity = replacementStorageQuantity -
        ExactQuantity.ofStorageFromUserProvided(
            revisedUserProvidedQuantityDelivered, revisedStorageUnitsPerUserProvidedUnit
        )
}
