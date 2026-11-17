package app.myliberty.easy_records.locations.business.retail.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.api.ReplacementDeliveryLineDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.stock.api.read.DeliveryLineStockInspector
import app.myliberty.easy_records.locations.business.retail.stock.api.revision.StockReplacementLayerRecorder
import app.myliberty.easy_records.locations.business.retail.stock.api.revision.StockRevisionCommitter
import app.myliberty.easy_records.util.business.reference_number.ReferenceNumberGenerator
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

data class StockRevisionOutcome(
    val stockRevision: StockRevisionEntity,
    val consumerCostChanges: List<ConsumerCostChange>,
)

@Component
@TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
class DeliveryLineStockReviser(
    private val deliveryLineStockInspector: DeliveryLineStockInspector,
    private val stockRevisionCommitter: StockRevisionCommitter,
    private val stockReplacementLayerRecorder: StockReplacementLayerRecorder,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val resetPlanner: ResetPlanner,
    private val replayPlanner: ReplayPlanner,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val stockRevisionRepository: StockRevisionRepository,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
) {

    fun revise(
        replacementLine: ReplacementDeliveryLineDto,
        locationProductId: UUID,
        reason: String
    ): StockRevisionOutcome {
        val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId)
        val stockRevisionReferenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            replacementLine.deliveryReferenceNumber, replacementLine.revisedLineReferenceNumber
        )
        val consumptionsOnRevisedLayer = netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)
        RestatableConsumers.requireAllRestatable(consumptionsOnRevisedLayer)

        stockRevisionCommitter.commitReset(
            resetPlanner.plan(
                revisedStockEntryId,
                consumptionsOnRevisedLayer,
                stockRevisionReferenceNumber,
                quantityBreakdown
            ),
            quantityBreakdown
        )
        val replacementStockEntryId = recordReplacementLayer(
            replacementLine, locationProductId, stockRevisionReferenceNumber, revisedStockEntryId
        )
        val consumerReplayPlans = replayPlanner.planReplayForExecution(
            consumptionsOnRevisedLayer, stockRevisionReferenceNumber, quantityBreakdown
        )
        stockRevisionCommitter.commitReplay(consumerReplayPlans, replacementStockEntryId, quantityBreakdown)

        val stockRevision = stockRevisionRepository.save(
            StockRevisionEntity(
                sourceType = StockRevisionSourceType.DELIVERY_LINE,
                sourceReferenceNumber = replacementLine.replacementLineReferenceNumber,
                revisedStockEntryId = revisedStockEntryId,
                replacementStockEntryId = replacementStockEntryId,
                reason = reason,
            ).also { it.referenceNumber = stockRevisionReferenceNumber }
        )
        return StockRevisionOutcome(stockRevision, ConsumerCostChange.ofPlans(consumerReplayPlans))
    }

    private fun recordReplacementLayer(
        replacementLine: ReplacementDeliveryLineDto,
        locationProductId: UUID,
        stockRevisionReferenceNumber: String,
        revisedStockEntryId: UUID
    ): UUID = stockReplacementLayerRecorder.record(
        DeliveryReplacementLayerRequests.of(
            locationProductId = locationProductId,
            deliveryReferenceNumber = replacementLine.deliveryReferenceNumber,
            replacementLineReferenceNumber = replacementLine.replacementLineReferenceNumber,
            stockRevisionReferenceNumber = stockRevisionReferenceNumber,
            userProvidedQuantityDelivered = replacementLine.userProvidedQuantityDelivered,
            unitId = replacementLine.unitId,
            storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
            unitCost = replacementLine.unitCost,
            revisedStockEntryId = revisedStockEntryId,
        )
    )
}
