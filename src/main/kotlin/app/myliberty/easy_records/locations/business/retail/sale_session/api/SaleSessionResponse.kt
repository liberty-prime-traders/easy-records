package app.myliberty.easy_records.locations.business.retail.sale_session.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleVoidInfoDto
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionResponseDto(
    val id: UUID,
    val saleId: UUID?,
    val referenceNumber: String?,
    val createdBy: String,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedBy: String,
    val lastAccessedAt: OffsetDateTime,
    val contactId: UUID,
    val contactLabel: String,
    val walkInCustomer: Boolean,
    val soldBy: String?,
    val saleStatus: SaleStatus,
    val paymentStatus: PaymentStatus,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val saleVoidInfo: SaleVoidInfoDto?,
    val saleLines: List<SaleSessionLineResponse>,
    val saleAdjustments: List<SaleSessionAdjustmentResponse>,
    val salePayments: List<SaleSessionPaymentResponse>,
    val totals: SaleSessionTotals,
    val uiOptions: SaleSessionUiOptions,
    val kind: SaleSessionKind,
    val consolidationIssues: List<ConsolidationIssueResponse>,
    val removedSaleLines: List<RemovedSaleLineResponse>,
)

data class SaleSessionUiOptions(
    val canAddLines: Boolean,
    val canEditLines: Boolean,
    val canChangeContact: Boolean,
    val canSaveDraft: Boolean,
    val canConfirm: Boolean,
    val saveDraftBlockers: List<String>,
    val confirmBlockers: List<String>,
    val allowedDiscardAction: SaleDiscardAction?,
    val canCopySale: Boolean,
    val canAddPaymentsToSale: Boolean,
    val showUnreservedChangesWarning: Boolean,
    val showActiveUserWarning: Boolean,
    val canGenerateConsolidatedSale: Boolean,
)

data class SaleSessionLineResponse(
    val identity: SessionIdentity,
    val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    @get:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
    val unitPriceOverride: BigDecimal,
    val netUnitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    @get:JsonProperty("baseQuantity")
    val quantityInBaseUnit: BigDecimal,
    @get:JsonProperty("quantityOnHand")
    val quantityOnHandInBaseUnit: BigDecimal,
    @get:JsonProperty("quantityReserved")
    val quantityReservedInBaseUnit: BigDecimal,
    @get:JsonProperty("quantityAvailable")
    val quantityAvailableInBaseUnit: BigDecimal,
    val storageQuantityOnHand: BigDecimal,
    val storageQuantityReserved: BigDecimal,
    val storageQuantityAvailable: BigDecimal,
    val quantityOnHandBreakdown: String,
    val quantityAvailableBreakdown: String,
    val productActive: Boolean,
    val rank: BigDecimal,
)

data class SaleSessionAdjustmentResponse(
    val identity: SessionIdentity,
    val relatedSaleLineIdentity: SessionIdentity?,
    val adjustmentReason: String?,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val calculatedAmount: BigDecimal,
    val note: String?,
    val approvedBy: String?,
)

data class SaleSessionPaymentResponse(
    val identity: SessionIdentity,
    val paymentMethod: String,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String?,
)

data class RemovedSaleLineResponse(
    val identity: SessionIdentity,
    val locationProductId: UUID,
    val productLabel: String,
    @get:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val sourceSaleReference: String,
)
