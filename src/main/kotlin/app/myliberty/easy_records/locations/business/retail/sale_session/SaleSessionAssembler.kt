package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleVoidInfoDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionResponseDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionSummaryDto
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.PendingConsolidationAssessment
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.PendingConsolidationAssessor
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.SystemAdjustmentReason
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.enums.SystemContact
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class SaleSessionAssembler(
    private val contactService: ContactService,
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val adjustmentReasonService: AdjustmentReasonService,
    private val sessionToResponseMapper: SessionToResponseMapper,
    private val saleDataFetcher: SaleDataFetcher,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    private val activeWarningSeconds: Long = 300

    fun buildResponse(saleSession: SaleSession): SaleSessionResponseDto {
        val saleVoidInfo: SaleVoidInfoDto? = requireSaleVoidInfoIfVoided(saleSession)
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val inactiveLocationProductIds = locationProductDataFetcher.findInactiveIds(
            saleSession.saleLines.map { it.locationProductId }.toSet()
        )
        val pendingConsolidationAssessment =
            assessIfPending(saleSession, inactiveLocationProductIds, unitConversionGraph)
        val sessionMappingContext = SaleSessionMappingContext(
            contactLabel = contactNameFor(saleSession.header.contactId),
            walkInCustomer = saleSession.header.contactId == SystemContact.WALK_IN.id,
            showActiveUserWarning = showActiveUserWarning(saleSession),
            showUnreservedChangesWarning = saleSession.showUnreservedChangesWarning,
            saleVoidInfo = saleVoidInfo,
            consolidationIssues = pendingConsolidationAssessment?.consolidationIssues ?: emptyList(),
            canGenerateConsolidatedSale = pendingConsolidationAssessment?.canGenerateConsolidatedSale ?: false,
            inactiveLocationProductIds = inactiveLocationProductIds,
            removedSaleLines = saleSession.pendingConsolidation?.removedSaleLineResponses() ?: emptyList(),
        )
        val adjustmentMappingContext = AdjustmentMappingContext(
            adjustmentReasonNamesById = adjustmentReasonService.getReasonNamesById(),
            saleSessionLines = saleSession.saleLines,
        )
        val lineMappingContext = buildLineMappingContext(saleSession, inactiveLocationProductIds, unitConversionGraph)
        return sessionToResponseMapper.toResponseDto(
            saleSession,
            sessionMappingContext,
            adjustmentMappingContext,
            lineMappingContext,
            paymentMethodDataFetcher.getNamesById(),
        )
    }

    private fun assessIfPending(
        saleSession: SaleSession,
        inactiveLocationProductIds: Set<UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): PendingConsolidationAssessment? =
        if (saleSession.kind == SaleSessionKind.PENDING_CONSOLIDATION) {
            PendingConsolidationAssessor.assess(saleSession, inactiveLocationProductIds, unitConversionGraph)
        } else {
            null
        }

    private fun requireSaleVoidInfoIfVoided(saleSession: SaleSession): SaleVoidInfoDto? {
        if (saleSession.originalStatus != SaleStatus.VOIDED) return null
        val saleId = saleSession.requiredSaleId()
        return saleDataFetcher.getSaleVoidInfo(saleId)
            ?: throw LibertyGenericException("Voided sale $saleId is missing its sale void record")
    }

    private fun buildLineMappingContext(
        saleSession: SaleSession,
        inactiveLocationProductIds: Set<UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): LineMappingContext {
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }

        val unitPriceOverrideAdjustmentsByLineKey = saleSession.saleAdjustments
            .filter { it.isPriceOverride(priceOverrideReasonId) }
            .associateBy { it.requiredRelatedSaleLineKey() }

        val unitPriceOverrideByLineKey = saleSessionLinesByKey.mapValues { (lineKey, saleSessionLine) ->
            unitPriceOverrideAdjustmentsByLineKey[lineKey]?.value ?: saleSessionLine.unitPrice
        }

        val lineAdjustmentsByLineKey = saleSession.saleAdjustments
            .filter { it.relatedSaleLineIdentity != null }
            .groupBy { it.requiredRelatedSaleLineKey() }

        val netLineTotalByLineKey = saleSessionLinesByKey.mapValues { (lineKey, saleSessionLine) ->
            val lineAdjustments = lineAdjustmentsByLineKey[lineKey] ?: emptyList()
            val netAdjustmentAmount = lineAdjustments.sumOf { saleSessionAdjustment ->
                val calculatedAmount = SaleSessionTotalsCalculator.calculateAdjustmentAmount(saleSessionAdjustment, saleSession.saleLines)
                when (saleSessionAdjustment.direction) {
                    AdjustmentDirection.DISCOUNT -> calculatedAmount.negate()
                    AdjustmentDirection.SURCHARGE -> calculatedAmount
                    AdjustmentDirection.BOTH -> throw IllegalStateException("A session adjustment is never BOTH")
                }
            }
            saleSessionLine.lineTotal + netAdjustmentAmount
        }

        val netUnitPriceByLineKey = netLineTotalByLineKey.mapValues { (lineKey, netLineTotal) ->
            Decimals.Scale4.divide(netLineTotal, saleSessionLinesByKey.getValue(lineKey).userProvidedQuantity)
        }

        val storageUnitsPerBaseUnitByBaseUnitId = saleSession.saleLines
            .map { it.baseUnitId }
            .distinct()
            .associateWith { unitConversionGraph.getSizeInRoot(it) }
        return LineMappingContext(
            unitPriceOverrideByLineKey = unitPriceOverrideByLineKey,
            netUnitPriceByLineKey = netUnitPriceByLineKey,
            netLineTotalByLineKey = netLineTotalByLineKey,
            storageUnitsPerBaseUnitByBaseUnitId = storageUnitsPerBaseUnitByBaseUnitId,
            breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
                saleSession.saleLines.map { it.locationProductId }.distinct()
            ),
            inactiveLocationProductIds = inactiveLocationProductIds,
        )
    }

    private fun showActiveUserWarning(saleSession: SaleSession): Boolean {
        if (saleSession.lastAccessedById == SessionContextProvider.getUserId()) return false
        val now = DateTimes.Offset.Now.organization()
        val warningThreshold = Duration.ofSeconds(activeWarningSeconds)
        return Duration.between(saleSession.lastAccessedAt, now) <= warningThreshold
    }

    fun buildSummaries(saleSessions: List<SaleSession>): List<SaleSessionSummaryDto> {
        if (saleSessions.isEmpty()) return emptyList()
        return saleSessions.map { session ->
            sessionToResponseMapper.toSummaryDto(session, contactNameFor(session.header.contactId))
        }
    }

    private fun contactNameFor(contactId: UUID): String {
        if (contactId == SystemContact.WALK_IN.id) return "Walk-In Customer"
        return contactService.getContactById(contactId).identity.displayName
    }
}
