package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSaveRequest(
    val saleId: UUID?,
    val expectedVersion: Long?,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val saleLines: List<SaleLineSaveRequest>,
    val saleAdjustments: List<SaleAdjustmentSaveRequest>,
    val salePayments: List<SalePaymentSaveRequest>,
    val preAllocatedReferenceNumber: String? = null,
)

data class SaleLineSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val locationProductId: UUID,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
)

data class SaleAdjustmentSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val relatedSaleLineClientKey: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)

data class SalePaymentSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
)

