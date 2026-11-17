package app.myliberty.easy_records.locations.business.retail.stock_reservation

import java.math.BigDecimal
import java.util.UUID

data class ProductReservationRow(
    val locationProductId: UUID,
    val saleId: UUID,
    val storageQuantityReserved: BigDecimal,
)
