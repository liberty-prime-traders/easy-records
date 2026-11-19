package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductLineWithPrice
import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionLine(
    val identity: SessionIdentity,
    override val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    override val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    override val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
    val storageQuantityOnHand: BigDecimal = BigDecimal.ZERO,
    val storageQuantityReserved: BigDecimal = BigDecimal.ZERO,
    val storageQuantityAvailable: BigDecimal = BigDecimal.ZERO,
    val rank: BigDecimal = BigDecimal.ZERO,

) : ProductLineWithPrice {

    override val lineKey: UUID = identity.key()

    companion object {
        // Multiplies first and divides last: multiply is exact, so the single division is the only rounding.
        fun lineTotal(defaultSalePrice: BigDecimal, lineQuantities: LineQuantities): BigDecimal =
            defaultSalePrice.multiply(lineQuantities.userProvidedQuantity)
                .multiply(BigDecimal.valueOf(lineQuantities.storageUnitsPerUserProvidedUnit))
                .let { storageValue -> Decimals.MoneyScale.divide(storageValue, BigDecimal.valueOf(lineQuantities.storageUnitsPerBaseUnit)) }
    }
}
