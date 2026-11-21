package app.myliberty.easy_records.util.quantity

import app.myliberty.easy_records.util.business.ConversionRatio
import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.math.RoundingMode

object BaseUnitDisplay {

    fun conversionFactor(storageUnitsPerUserProvidedUnit: Long, storageUnitsPerBaseUnit: Long): BigDecimal =
        ConversionRatio(storageUnitsPerUserProvidedUnit, storageUnitsPerBaseUnit).factor()

    fun baseQuantity(
        userProvidedQuantity: BigDecimal,
        storageUnitsPerUserProvidedUnit: Long,
        storageUnitsPerBaseUnit: Long,
        roundingMode: RoundingMode
    ): BigDecimal = ExactQuantity.ofStorageFromUserProvided(userProvidedQuantity, storageUnitsPerUserProvidedUnit)
        .toDisplayQuantity(storageUnitsPerBaseUnit, roundingMode)

    fun storageQuantityInBaseUnit(storageQuantity: BigDecimal, storageUnitsPerBaseUnit: Long, roundingMode: RoundingMode): BigDecimal =
        ExactQuantity.ofStorage(storageQuantity).toDisplayQuantity(storageUnitsPerBaseUnit, roundingMode)

    fun pricePerBaseUnit(total: BigDecimal, storageQuantity: BigDecimal, storageUnitsPerBaseUnit: Long): BigDecimal? =
        if (storageQuantity.signum() == 0) null
        else Decimals.Scale4.divide(total.multiply(BigDecimal.valueOf(storageUnitsPerBaseUnit)), storageQuantity)
}
