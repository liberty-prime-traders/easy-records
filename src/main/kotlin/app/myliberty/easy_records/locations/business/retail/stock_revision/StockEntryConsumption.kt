package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockMovementDto
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityAsEntered
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockEntryConsumption(
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val consumer: StockConsumer,
    val quantityAsEntered: QuantityAsEntered,
    val orderOfConsumption: OrderOfConsumption,
    val netConsumedStorageQuantity: ExactQuantity,
    val netConsumedCost: BigDecimal
) {
    val stockMovementOrigin: StockMovementOrigin = consumer.stockMovementOrigin

    companion object {
        fun requireOnOneLayer(stockEntryConsumptions: List<StockEntryConsumption>) =
            require(stockEntryConsumptions.map { it.stockEntryId }.distinct().size == 1) {
                "A stock revision replaces one layer, so its consumptions must all sit on one layer."
            }
    }
}

data class StockConsumer(
    val stockMovementOrigin: StockMovementOrigin,
    val movementType: MovementType,
    val adjustmentDetails: StockAdjustmentMovementDetails?,
) {
    companion object {
        fun of(stockMovementDto: StockMovementDto) = StockConsumer(
            stockMovementOrigin = StockMovementOrigin.of(stockMovementDto),
            movementType = stockMovementDto.movementType,
            adjustmentDetails = stockMovementDto.adjustmentDetails,
        )
    }
}

data class StockMovementOrigin(
    val documentReference: String?,
    val detailReference: String?,
) {
    val displayText: String = listOfNotNull(documentReference, detailReference).joinToString("/")

    fun requiredDocumentReference(): String = checkNotNull(documentReference) {
        "A stock movement of $displayText has no document reference."
    }

    companion object {
        fun of(stockMovementDto: StockMovementDto) = StockMovementOrigin(
            stockMovementDto.documentReference,
            stockMovementDto.detailReference
        )
    }
}

data class OrderOfConsumption(val createdOn: OffsetDateTime?, val id: UUID?) : Comparable<OrderOfConsumption> {

    override fun compareTo(other: OrderOfConsumption): Int = comparator.compare(this, other)

    companion object {
        private val comparator: Comparator<OrderOfConsumption> =
            compareBy(nullsLast()) { orderOfConsumption: OrderOfConsumption -> orderOfConsumption.createdOn }
                .thenBy(nullsLast()) { orderOfConsumption: OrderOfConsumption -> orderOfConsumption.id }

        fun of(stockMovementDto: StockMovementDto) = OrderOfConsumption(stockMovementDto.createdOn, stockMovementDto.id)
    }
}
