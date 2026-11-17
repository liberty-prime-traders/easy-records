package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.locations.business.retail.purchase.api.payment.PurchasePayableCalculator
import app.myliberty.easy_records.locations.business.retail.purchase.api.bulk_payment.PurchaseBulkPaymentResponseDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentResponseDto
import java.math.BigDecimal
import java.util.UUID

object PurchaseBulkPaymentAssembler {

  fun buildResponses(
    purchaseEntities: List<PurchaseEntity>,
    ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
    paidTotalsByPurchaseId: Map<UUID, BigDecimal>,
    newSupplierPaymentsByPurchaseId: Map<UUID, SupplierPaymentResponseDto> = emptyMap()
  ): List<PurchaseBulkPaymentResponseDto> =
    purchaseEntities.map { purchaseEntity ->
      val purchaseId = purchaseEntity.id!!
      val ceiling = ceilingsByPurchaseId.getValue(purchaseId)
      val paidTotal = paidTotalsByPurchaseId.getValue(purchaseId)
      val newPayment = newSupplierPaymentsByPurchaseId[purchaseId]
      PurchaseBulkPaymentResponseDto(
        id = purchaseId,
        referenceNumber = purchaseEntity.requiredReference(),
        purchaseStatus = purchaseEntity.purchaseStatus,
        dateOrdered = purchaseEntity.dateOrdered,
        orderedTotal = ceiling.orderedTotal,
        paymentCeiling = ceiling.amount,
        paidTotal = paidTotal,
        payable = PurchasePayableCalculator.calculatePayable(ceiling.amount, paidTotal),
        paymentStatus = newPayment?.updatedPurchasePaymentStatus ?: purchaseEntity.paymentStatus,
        appliedAmount = newPayment?.amount ?: BigDecimal.ZERO,
        supplierPaymentId = newPayment?.id,
        paymentReferenceNumber = newPayment?.referenceNumber
      )
    }
}
