package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.sale.api.SaleVoidInfoDto
import app.myliberty.easy_records.locations.business.sale_session.api.ConsolidationIssueResponse
import app.myliberty.easy_records.locations.business.sale_session.api.RemovedSaleLineResponse
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionMappingContext(
    val contactLabel: String,
    val walkInCustomer: Boolean,
    val showActiveUserWarning: Boolean,
    val showUnreservedChangesWarning: Boolean,
    val saleVoidInfo:  SaleVoidInfoDto?,
    val consolidationIssues: List<ConsolidationIssueResponse>,
    val canGenerateConsolidatedSale: Boolean,
    val inactiveLocationProductIds: Set<UUID>,
    val removedSaleLines: List<RemovedSaleLineResponse>,
)

data class AdjustmentMappingContext(
    private val adjustmentReasonNamesById: Map<UUID, String>,
    private val saleSessionLines: List<SaleSessionLine>,
) {
    fun reasonLabel(adjustmentReasonId: UUID): String? = adjustmentReasonNamesById[adjustmentReasonId]
    fun calculatedAmount(saleSessionAdjustment: SaleSessionAdjustment): BigDecimal =
        SaleSessionTotalsCalculator.calculateAdjustmentAmount(saleSessionAdjustment, saleSessionLines)
}

data class LineMappingContext(
    private val unitPriceOverrideByLineKey: Map<UUID, BigDecimal>,
    private val netUnitPriceByLineKey: Map<UUID, BigDecimal>,
    private val netLineTotalByLineKey: Map<UUID, BigDecimal>,
    private val storageUnitsPerBaseUnitByBaseUnitId: Map<UUID, Long>,
    private val breakdownByLocationProductId: Map<UUID, QuantityBreakdown>,
    private val inactiveLocationProductIds: Set<UUID>,
) {
    fun productActiveFor(saleSessionLine: SaleSessionLine): Boolean =
        saleSessionLine.locationProductId !in inactiveLocationProductIds

    fun storageUnitsPerBaseUnitFor(saleSessionLine: SaleSessionLine): Long =
        storageUnitsPerBaseUnitByBaseUnitId.getValue(saleSessionLine.baseUnitId)

    fun breakdownFor(saleSessionLine: SaleSessionLine): QuantityBreakdown =
        breakdownByLocationProductId.getValue(saleSessionLine.locationProductId)

    fun unitPriceOverrideFor(saleSessionLine: SaleSessionLine): BigDecimal =
        unitPriceOverrideByLineKey.getValue(saleSessionLine.identity.key())

    fun netUnitPriceFor(saleSessionLine: SaleSessionLine): BigDecimal =
        netUnitPriceByLineKey.getValue(saleSessionLine.identity.key())

    fun netLineTotalFor(saleSessionLine: SaleSessionLine): BigDecimal =
        netLineTotalByLineKey.getValue(saleSessionLine.identity.key())
}
