package app.myliberty.easy_records.locations.business.purchase.api.delivery

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.purchase.PurchaseAssembler
import app.myliberty.easy_records.locations.business.purchase.PurchaseLineRepository
import app.myliberty.easy_records.locations.business.purchase.PurchaseRepository
import app.myliberty.easy_records.locations.business.purchase.PurchaseStatusResolver
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.locations.business.supplier_payment.api.PurchasePaymentStatusService
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
class PurchaseDeliveryRevisionApplier(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
) {

    // The caller holds the purchase lock and has already saved the replacement delivery line,
    // so the ceiling sums see it.
    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun apply(purchaseId: UUID, purchaseLineId: UUID, storageQuantityDelivered: ExactQuantity): PurchaseResponseDto {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val purchaseLine = purchaseLines.first { it.id == purchaseLineId }
        purchaseLine.storageQuantityDelivered = storageQuantityDelivered.toStorageQuantity()
        purchaseLineRepository.save(purchaseLine)
        purchase.purchaseStatus = PurchaseStatusResolver.resolveAfterQuantityChange(purchaseLines)
        purchaseRepository.save(purchase)

        val purchaseResponse = purchaseAssembler.buildResponse(purchase, purchaseLines)
        return purchaseResponse.copy(
            paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(
                purchaseId,
                purchaseResponse.paymentCeiling
            )
        )
    }
}
