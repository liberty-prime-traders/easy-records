package app.myliberty.easy_records.locations.business.stock_transfer

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import app.myliberty.easy_records.configuration.serializer.StripZeroesAndRound
import java.math.BigDecimal
import java.util.UUID

// quantity and quantityReceived are whole-line figures repeated on every cost-group row of a dispatched line: never sum them
data class ReconciledTransferLine(
    val dispatchLineReferenceNumber: String,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val unitCost: BigDecimal?,
    val quantityReceived: BigDecimal?,
    val quantityAvailable: BigDecimal? = null,
    val totalCost: BigDecimal?,
    val storageQuantity: BigDecimal? = null,
    val storageBreakdownText: String? = null,
)
