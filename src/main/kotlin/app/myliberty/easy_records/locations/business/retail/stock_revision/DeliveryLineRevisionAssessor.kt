package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryLineReplaceRequest
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryLineReplacer
import app.myliberty.easy_records.locations.business.retail.delivery.api.ValidatedDeliveryLineRevisionDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.stock.api.read.DeliveryLineStockInspector
import app.myliberty.easy_records.locations.business.retail.stock.api.revision.StockReplacementLayerPreviewer
import app.myliberty.easy_records.locations.business.retail.stock_revision.api.DeliveryLineRevisionRequest
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineRevisionAssessor(
    private val purchaseDeliveryLineReplacer: PurchaseDeliveryLineReplacer,
    private val deliveryLineStockInspector: DeliveryLineStockInspector,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val stockReplacementLayerPreviewer: StockReplacementLayerPreviewer,
    private val replayPlanner: ReplayPlanner,
    private val reservationShortfallChecker: ReservationShortfallChecker,
) {

    fun assess(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionAssessment {
        val validatedDeliveryLineRevision = purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
            PurchaseDeliveryLineReplaceRequest(
                revisedDeliveryLineId = deliveryLineRevisionRequest.revisedDeliveryLineId,
                userProvidedQuantityDelivered = deliveryLineRevisionRequest.userProvidedQuantityDelivered,
                unitId = deliveryLineRevisionRequest.unitId,
                unitCost = deliveryLineRevisionRequest.unitCost,
            )
        )
        val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(
            validatedDeliveryLineRevision.locationProductId
        )
        if (validatedDeliveryLineRevision.problems.isNotEmpty()) {
            return DeliveryLineRevisionAssessment(
                validatedDeliveryLineRevision, quantityBreakdown,
                ReplayResult(emptyList(), emptyList()), null
            )
        }
        val replay = replayOnRevisedLayer(validatedDeliveryLineRevision, quantityBreakdown)
        val reservationShortfall = if (replay.replayResult.problems.isEmpty()) {
            reservationShortfallChecker.shortfallAfterPendingChange(
                validatedDeliveryLineRevision.locationProductId,
                validatedDeliveryLineRevision.storageQuantityChange,
                quantityBreakdown
            )
        } else {
            null
        }
        return DeliveryLineRevisionAssessment(
            validatedDeliveryLineRevision, quantityBreakdown, replay.replayResult,
            reservationShortfall, replay.replacementStockEntryId
        )
    }

    private fun replayOnRevisedLayer(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        quantityBreakdown: QuantityBreakdown
    ): ReplayOnRevisedLayer {
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            validatedDeliveryLineRevision.deliveryReferenceNumber,
            validatedDeliveryLineRevision.revisedLineReferenceNumber
        )
        val consumptionsOnRevisedLayer = netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)
        val problemsOfUnrestatableConsumers = RestatableConsumers.problemsOf(consumptionsOnRevisedLayer)
        if (problemsOfUnrestatableConsumers.isNotEmpty()) {
            return ReplayOnRevisedLayer(ReplayResult(emptyList(), problemsOfUnrestatableConsumers), null)
        }
        val replacementLayerPreview = stockReplacementLayerPreviewer.previewReplacement(
            replacementLayerRequestOf(validatedDeliveryLineRevision, revisedStockEntryId)
        )
        val replayResult = replayPlanner.planReplayForPreview(
            consumptionsOnRevisedLayer, replacementLayerPreview.fifoEntries, ASSESSMENT_REFERENCE, quantityBreakdown
        )
        return ReplayOnRevisedLayer(replayResult, replacementLayerPreview.replacementStockEntryId)
    }

    private fun replacementLayerRequestOf(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        revisedStockEntryId: UUID
    ) = DeliveryReplacementLayerRequests.of(
        locationProductId = validatedDeliveryLineRevision.locationProductId,
        deliveryReferenceNumber = validatedDeliveryLineRevision.deliveryReferenceNumber,
        replacementLineReferenceNumber = ASSESSMENT_REFERENCE,
        stockRevisionReferenceNumber = ASSESSMENT_REFERENCE,
        userProvidedQuantityDelivered = validatedDeliveryLineRevision.userProvidedQuantityDelivered,
        unitId = validatedDeliveryLineRevision.unitId,
        storageUnitsPerUserProvidedUnit = validatedDeliveryLineRevision.storageUnitsPerUserProvidedUnit,
        unitCost = validatedDeliveryLineRevision.unitCost,
        revisedStockEntryId = revisedStockEntryId,
    )

    private data class ReplayOnRevisedLayer(val replayResult: ReplayResult, val replacementStockEntryId: UUID?)

    private companion object {
        const val ASSESSMENT_REFERENCE = "PREVIEW"
    }
}
