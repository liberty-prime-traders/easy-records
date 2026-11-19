package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import java.math.BigDecimal
import java.util.UUID

data class SaleLineStockRequest(
    val saleLineId: UUID,
    val saleLineReferenceNumber: String,
    val locationProductId: UUID,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
)
