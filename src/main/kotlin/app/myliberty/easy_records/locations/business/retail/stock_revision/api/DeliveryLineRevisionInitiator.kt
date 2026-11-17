package app.myliberty.easy_records.locations.business.stock_revision.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import app.myliberty.easy_records.locations.business.delivery.api.ReplacementDeliveryLineDto
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.locations.business.stock_revision.DeliveryLineRevisionAssessor
import app.myliberty.easy_records.locations.business.stock_revision.DeliveryLineStockReviser
import app.myliberty.easy_records.locations.business.stock_revision.RevisionCostPropagator
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.BlockedChangeException
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class DeliveryLineRevisionInitiator(
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseDeliveryLineReplacer: PurchaseDeliveryLineReplacer,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val purchaseDeliveryRevisionApplier: PurchaseDeliveryRevisionApplier,
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
    private val deliveryLineStockReviser: DeliveryLineStockReviser,
    private val revisionCostPropagator: RevisionCostPropagator,
) {

    fun initiateDeliveryLineRevision(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): PurchaseResponseDto {
        val reason = StringUtils.getValueOrException(
            deliveryLineRevisionRequest.reason,
            "Enter the reason for this change."
        )
        val revisedDeliveryLineId = deliveryLineRevisionRequest.revisedDeliveryLineId
        val (purchaseId, purchaseLineId) = purchaseDeliveryDataFetcher.getOwners(revisedDeliveryLineId)
        val locationProductId = purchaseDataFetcher.getLocationProductIdOfLine(purchaseLineId)
        lockPurchaseThenProduct(purchaseId, locationProductId)

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)
        BlockedChangeException.throwIfAny(assessment.blockingProblems)

        val replacementLine = purchaseDeliveryLineReplacer.replace(assessment.validatedDeliveryLineRevision)
        val stockRevisionOutcome = deliveryLineStockReviser.revise(replacementLine, locationProductId, reason)
        revisionCostPropagator.propagate(stockRevisionOutcome, purchaseId, replacementLine)
        return applyToPurchase(purchaseId, replacementLine)
    }

    // The id lookups read scalars, so no entity is cached before the locks.
    // Purchase first, the order a delivery takes, so the two cannot deadlock.
    private fun lockPurchaseThenProduct(purchaseId: UUID, locationProductId: UUID) {
        purchaseDataFetcher.lockPurchase(purchaseId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
    }

    private fun applyToPurchase(purchaseId: UUID, replacementLine: ReplacementDeliveryLineDto): PurchaseResponseDto =
        purchaseDeliveryRevisionApplier.apply(
            purchaseId = purchaseId,
            purchaseLineId = replacementLine.purchaseLineId,
            storageQuantityDelivered = purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(
                replacementLine.purchaseLineId
            ),
        )
}
