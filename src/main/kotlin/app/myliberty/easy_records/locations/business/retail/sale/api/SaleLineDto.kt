package app.myliberty.easy_records.locations.business.retail.sale.api

import java.math.BigDecimal
import java.util.UUID

data class SaleLineDto(
    val id: UUID,
    val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
)
