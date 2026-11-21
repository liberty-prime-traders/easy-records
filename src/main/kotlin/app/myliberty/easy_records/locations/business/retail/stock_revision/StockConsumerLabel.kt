package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType

object StockConsumerLabel {

    fun of(movementType: MovementType): String = when (movementType) {
        MovementType.SALE -> "Sale"
        MovementType.TRANSFER_OUT -> "Transfer"
        MovementType.STOCK_ADJUSTMENT_DECREASE -> "Stock adjustment"
        MovementType.SUPPLIER_RETURN -> "Return to supplier"
        else -> movementType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
