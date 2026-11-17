package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineEntity
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryRepository
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryRevisionValidator
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUnitRequestDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.DeliveryHandlerForPurchase
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.util.exceptions.BlockedChangeException
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

// Writes the line rows only; stock, ledger and purchase totals are the caller's. The caller must hold the product lock
// for the checks to hold.
@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryLineReplacer(
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
    private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
    private val purchaseDeliveryRevisionValidator: PurchaseDeliveryRevisionValidator,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun validateRevisionReportingProblems(
        purchaseDeliveryLineReplaceRequest: PurchaseDeliveryLineReplaceRequest
    ): ValidatedDeliveryLineRevisionDto {
        val revisedLine = requiredRevisedLine(purchaseDeliveryLineReplaceRequest.revisedDeliveryLineId)
        val delivery = purchaseDeliveryRepository.getReferenceById(revisedLine.purchaseDeliveryId)
        val purchaseLineDto = deliveryHandlerForPurchase.getDeliveryContext(delivery.purchaseId)
            .purchaseLineById.getValue(revisedLine.purchaseLineId)
        val replacementUserProvidedQuantity =
            purchaseDeliveryLineReplaceRequest.userProvidedQuantityDelivered.toBigDecimal()
        val replacementStorageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnitOf(
            purchaseLineDto.locationProductId, purchaseDeliveryLineReplaceRequest.unitId
        )
        val replacementStorageQuantity = ExactQuantity.ofStorageFromUserProvided(
            replacementUserProvidedQuantity, replacementStorageUnitsPerUserProvidedUnit
        )
        val problems = purchaseDeliveryRevisionValidator.problemsOf(
            delivery.requiredReference(), purchaseLineDto,
            revisedLine, replacementStorageQuantity
        )
        return ValidatedDeliveryLineRevisionDto(
            revisedDeliveryLineId = purchaseDeliveryLineReplaceRequest.revisedDeliveryLineId,
            deliveryReferenceNumber = delivery.requiredReference(),
            revisedLineReferenceNumber = revisedLine.requiredReference(),
            purchaseLineId = revisedLine.purchaseLineId,
            locationProductId = purchaseLineDto.locationProductId,
            revisedUnitId = revisedLine.unitId,
            revisedStorageUnitsPerUserProvidedUnit = revisedLine.storageUnitsPerUserProvidedUnit,
            unitId = purchaseDeliveryLineReplaceRequest.unitId,
            storageUnitsPerUserProvidedUnit = replacementStorageUnitsPerUserProvidedUnit,
            revisedUserProvidedQuantityDelivered = revisedLine.userProvidedQuantityDelivered,
            revisedUnitCost = revisedLine.unitCost,
            userProvidedQuantityDelivered = replacementUserProvidedQuantity,
            unitCost = purchaseDeliveryLineReplaceRequest.unitCost.toBigDecimal(),
            problems = problems,
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun replace(validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto): ReplacementDeliveryLineDto {
        BlockedChangeException.throwIfAny(validatedDeliveryLineRevision.problems)
        val revisedLine = requiredRevisedLine(validatedDeliveryLineRevision.revisedDeliveryLineId)

        revisedLine.status = PurchaseDeliveryLineStatus.REVISED
        val replacementLine = purchaseDeliveryLineRepository.save(
            PurchaseDeliveryLineEntity(
                purchaseDeliveryId = revisedLine.purchaseDeliveryId,
                purchaseLineId = revisedLine.purchaseLineId,
                userProvidedQuantityDelivered = validatedDeliveryLineRevision.userProvidedQuantityDelivered,
                unitCost = validatedDeliveryLineRevision.unitCost,
                unitId = validatedDeliveryLineRevision.unitId,
                storageUnitsPerUserProvidedUnit = validatedDeliveryLineRevision.storageUnitsPerUserProvidedUnit,
            )
        )
        purchaseDeliveryLineRepository.save(revisedLine)

        return ReplacementDeliveryLineDto(
            deliveryReferenceNumber = validatedDeliveryLineRevision.deliveryReferenceNumber,
            revisedLineReferenceNumber = revisedLine.requiredReference(),
            replacementLineReferenceNumber = replacementLine.requiredReference(),
            purchaseLineId = replacementLine.purchaseLineId,
            userProvidedQuantityDelivered = replacementLine.userProvidedQuantityDelivered,
            unitCost = replacementLine.unitCost,
            unitId = replacementLine.unitId,
            storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
            revisedLineTotal = revisedLine.lineTotal(),
            replacementLineTotal = replacementLine.lineTotal(),
        )
    }

    private fun storageUnitsPerUserProvidedUnitOf(locationProductId: UUID, unitId: UUID): Long {
        val locationProductUnitRequest = LocationProductUnitRequestDto(locationProductId, unitId)
        return locationProductDataFetcher.getStorageUnitsByRequest(listOf(locationProductUnitRequest))
            .getValue(locationProductUnitRequest)
    }

    private fun requiredRevisedLine(revisedDeliveryLineId: UUID): PurchaseDeliveryLineEntity =
        purchaseDeliveryLineRepository.findById(revisedDeliveryLineId).orElseThrow {
            RtsGenericException("That delivery line could not be found. Refresh the page and try again.")
        }
}
