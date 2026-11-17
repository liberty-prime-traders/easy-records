package app.myliberty.easy_records.locations.business.retail.sale_adjustment.api

import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.util.UUID

data class SaleAdjustmentCreateDto(
    val saleLineKey: UUID? = null,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val adjustmentReasonId: UUID,
    val note: String? = null,
    val approvedById: UUID? = null
)
