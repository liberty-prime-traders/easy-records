package app.myliberty.easy_records.locations.business.retail.purchase.api.delivery

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseAssembler
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseEntity
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineRepository
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseMapper
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseRepository
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseStatusResolver
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseValidator
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

data class DeliveryLineQuantity(val purchaseLineId: UUID, val storageQuantity: ExactQuantity)

@Service
class DeliveryHandlerForPurchase(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchaseDataFetcher: PurchaseDataFetcher,
) {

    @TransactionalOnLocationSchema
    fun prepareForDelivery(purchaseId: UUID): PurchaseDeliveryContext {
        val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseId)
        PurchaseValidator.guardCanReceiveDelivery(purchase)
        return buildContext(purchase)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getDeliveryContext(purchaseId: UUID): PurchaseDeliveryContext {
        return buildContext(purchaseRepository.getReferenceById(purchaseId))
    }

    private fun buildContext(purchase: PurchaseEntity): PurchaseDeliveryContext {
        val lines = purchaseLineRepository.findByPurchaseId(purchase.id!!)
        return PurchaseDeliveryContext(
            purchaseId = purchase.id!!,
            purchaseReferenceNumber = purchase.requiredReference(),
            supplierId = purchase.supplierId,
            purchaseLineById = lines.associateBy { it.id!! }.mapValues {
                PurchaseMapper.purchaseLineEntityToDto(it.value)
            }
        )
    }
    
    @TransactionalOnLocationSchema
    fun commitDelivery(purchaseId: UUID, deliveryLines: List<DeliveryLineQuantity>): PurchaseResponseDto {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val lineById = purchaseLines.associateBy { it.id!! }
        val toSave = deliveryLines.map { (lineId, storageQuantity) ->
            lineById[lineId]?.also {
                it.storageQuantityDelivered = (ExactQuantity.ofStorage(it.storageQuantityDelivered) + storageQuantity).toStorageQuantity()
            } ?: throw LibertyGenericException("Purchase line $lineId not found")
        }
        purchaseLineRepository.saveAll(toSave)
        purchase.purchaseStatus = PurchaseStatusResolver.resolveAfterQuantityChange(purchaseLines)
        purchaseRepository.save(purchase)
        return purchaseAssembler.buildResponse(purchase, purchaseLines)
    }
}
