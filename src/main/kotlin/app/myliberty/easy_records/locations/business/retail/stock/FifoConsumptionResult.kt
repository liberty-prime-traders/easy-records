package app.myliberty.easy_records.locations.business.stock

import java.math.BigDecimal

data class FifoConsumptionResult(
    val movements: List<StockMovementEntity>,
    val touchedEntries: List<StockEntryEntity>,
    val cost: BigDecimal,
    val endingBalance: BigDecimal
)
