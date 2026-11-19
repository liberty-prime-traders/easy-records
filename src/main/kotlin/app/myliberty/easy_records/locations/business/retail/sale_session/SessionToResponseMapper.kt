package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustmentResponse
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLineResponse
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPaymentResponse
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionResponseDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionSummaryDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.util.UUID

@Mapper(config = LibertyMapperConfig::class, uses = [SaleSessionQualifierUtil::class])
interface SessionToResponseMapper {

    @Mapping(source = "header.referenceNumber", target = "referenceNumber")
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "lastAccessedById", target = "lastAccessedBy", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(target = "contactLabel", expression = "java(sessionMappingContext.getContactLabel())")
    @Mapping(target = "walkInCustomer", expression = "java(sessionMappingContext.getWalkInCustomer())")
    @Mapping(source = "sessionId", target = "id")
    @Mapping(source = "header.soldById", target = "soldBy", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "header.dateSold", target = "dateSold")
    @Mapping(source = "header.notes", target = "notes")
    @Mapping(target = "saleVoidInfo", expression = "java(sessionMappingContext.getSaleVoidInfo())")
    @Mapping(source = "originalStatus", target = "saleStatus")
    @Mapping(source = ".", target = "paymentStatus", qualifiedBy = [SaleSessionPaymentStatus::class])
    @Mapping(source = ".", target = "uiOptions", qualifiedBy = [SaleSessionUiOptionsBuild::class])
    @Mapping(target = "consolidationIssues", expression = "java(sessionMappingContext.getConsolidationIssues())")
    @Mapping(target = "removedSaleLines", expression = "java(sessionMappingContext.getRemovedSaleLines())")
    fun toResponseDto(
        saleSession: SaleSession,
        @Context sessionMappingContext: SaleSessionMappingContext,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
        @Context lineMappingContext: LineMappingContext,
        @Context paymentMethodNamesById: Map<UUID, String>,
    ): SaleSessionResponseDto

    @Mapping(target = "unitPriceOverride", source = ".", qualifiedBy = [LineUnitPriceOverride::class])
    @Mapping(target = "netUnitPrice", source = ".", qualifiedBy = [LineNetUnitPrice::class])
    @Mapping(target = "conversionFactor", source = ".", qualifiedBy = [LineConversionFactor::class])
    @Mapping(target = "quantityInBaseUnit", source = ".", qualifiedBy = [LineBaseQuantity::class])
    @Mapping(target = "quantityOnHandInBaseUnit", source = ".", qualifiedBy = [LineQuantityOnHandInBaseUnit::class])
    @Mapping(target = "quantityReservedInBaseUnit", source = ".", qualifiedBy = [LineQuantityReservedInBaseUnit::class])
    @Mapping(target = "quantityAvailableInBaseUnit", source = ".", qualifiedBy = [LineQuantityAvailableInBaseUnit::class])
    @Mapping(target = "quantityOnHandBreakdown", source = ".", qualifiedBy = [LineOnHandBreakdown::class])
    @Mapping(target = "quantityAvailableBreakdown", source = ".", qualifiedBy = [LineAvailableBreakdown::class])
    @Mapping(target = "productActive", source = ".", qualifiedBy = [LineProductActive::class])
    @Mapping(target = "lineTotal", source = ".", qualifiedBy = [LineNetTotal::class])
    fun toLineDto(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): SaleSessionLineResponse

    @Mapping(source = "adjustmentReasonId", target = "adjustmentReason", qualifiedBy = [AdjustmentReasonLabel::class])
    @Mapping(source = ".", target = "calculatedAmount", qualifiedBy = [AdjustmentCalculatedAmount::class])
    @Mapping(source = "approvedById", target = "approvedBy", qualifiedBy = [NullableFullName::class])
    fun toAdjustmentDto(
        saleSessionAdjustment: SaleSessionAdjustment,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
    ): SaleSessionAdjustmentResponse

    @Mapping(source = "paymentMethodId", target = "paymentMethod", qualifiedBy = [PaymentMethodName::class])
    fun toPaymentDto(
        saleSessionPayment: SaleSessionPayment,
        @Context paymentMethodNamesById: Map<UUID, String>,
    ): SaleSessionPaymentResponse

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "lastAccessedById", target = "lastAccessedBy", qualifiedBy = [NullableFullName::class])
    @Mapping(target = "contactLabel", expression = "java(contactLabel)")
    @Mapping(source = "totals.receivableTotal", target = "receivableTotal")
    @Mapping(source = "sessionId", target = "id")
    fun toSummaryDto(
        saleSession: SaleSession,
        @Context contactLabel: String,
    ): SaleSessionSummaryDto
}
