package app.myliberty.easy_records.locations.business.sale_adjustment.api

import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.util.UUID

data class SaleAdjustmentDto(
    val id: UUID,
    val saleLineId: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)

data class SaleAdjustmentSummaryDto(
    val saleLineId: UUID?,
    val direction: AdjustmentDirection,
    val calculatedAmount: BigDecimal,
)
