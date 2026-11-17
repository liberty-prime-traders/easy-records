package app.myliberty.easy_records.locations.business.stock

import java.math.BigDecimal

data class FifoRestorationResult(
    val movements: List<StockMovementEntity>,
    val touchedEntries: List<StockEntryEntity>,
    val endingBalance: BigDecimal
)
