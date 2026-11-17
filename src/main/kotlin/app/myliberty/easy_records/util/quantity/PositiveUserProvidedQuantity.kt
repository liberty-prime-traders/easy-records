package app.myliberty.easy_records.util.quantity

import com.fasterxml.jackson.annotation.JsonCreator
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.TypedDecimal
import java.math.BigDecimal

class PositiveUserProvidedQuantity private constructor(typedValue: BigDecimal) : TypedDecimal<PositiveUserProvidedQuantity>(typedValue) {

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(typedValue: BigDecimal): PositiveUserProvidedQuantity {
            val quantity = UserProvidedQuantity.of(typedValue).toBigDecimal()
            Decimals.requirePositive(quantity, "Quantity")
            return PositiveUserProvidedQuantity(quantity)
        }
    }
}
