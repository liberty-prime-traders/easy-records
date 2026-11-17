package app.myliberty.easy_records.locations.business.opening_stock.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity

/** One row of a bulk opening stock upload. Products and units are named by their human-facing
 * reference number and unit code; [defaultSalePrice] is written to the location product.
 * [unitCost] is priced per [costUnitCode], which defaults to [unitCode]. A zero [userProvidedQuantity] declares no
 * opening stock and only sets the price; any positive quantity needs a [unitCost]. */
data class OpeningStockBulkLineDto(
    val locationProductReferenceNumber: String,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: UserProvidedQuantity,
    val unitCode: String,
    val unitCost: PositiveMoney? = null,
    val costUnitCode: String? = null,
    val defaultSalePrice: Money
)
