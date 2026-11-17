package app.myliberty.easy_records.locations.business.sale_session.api

import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionAdjustment(
    val identity: SessionIdentity,
    val relatedSaleLineIdentity: SessionIdentity?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
) {
    fun isPriceOverride(priceOverrideReasonId: UUID): Boolean =
        adjustmentReasonId == priceOverrideReasonId && relatedSaleLineIdentity != null

    fun requiredRelatedSaleLineKey(): UUID = relatedSaleLineIdentity?.key()
        ?: error("Sale session adjustment ${identity.key()} is not tied to a sale line")
}
