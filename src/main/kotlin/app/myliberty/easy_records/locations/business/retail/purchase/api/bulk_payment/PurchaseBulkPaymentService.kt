package app.myliberty.easy_records.locations.business.purchase.api.bulk_payment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.purchase.PayablePurchaseFinder
import app.myliberty.easy_records.locations.business.purchase.PurchaseBulkPaymentAssembler
import app.myliberty.easy_records.locations.business.purchase.PurchaseBulkPaymentPlanner
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentDataFetcher
import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentService
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class PurchaseBulkPaymentService(
  private val purchaseDataFetcher: PurchaseDataFetcher,
  private val payablePurchaseFinder: PayablePurchaseFinder,
  private val purchaseBulkPaymentPlanner: PurchaseBulkPaymentPlanner,
  private val supplierPaymentService: SupplierPaymentService,
  private val supplierPaymentDataFetcher: SupplierPaymentDataFetcher,
  private val purchasePaymentCeilingService: PurchasePaymentCeilingService
) {

  companion object {
    const val MAX_PAID_PURCHASES = 20
  }

  @TransactionalOnLocationSchema(readOnly = true)
  fun loadPayable(purchaseBulkPaymentSearchDto: PurchaseBulkPaymentSearchDto): List<PurchaseBulkPaymentResponseDto> {
    val purchaseEntities = payablePurchaseFinder.findPayablePurchases(purchaseBulkPaymentSearchDto)
    if (purchaseEntities.isEmpty()) return emptyList()
    val purchaseIds = purchaseEntities.map { it.id!! }.toSet()
    return PurchaseBulkPaymentAssembler.buildResponses(
      purchaseEntities,
      purchasePaymentCeilingService.computeCeilings(purchaseIds),
      supplierPaymentDataFetcher.getPaidAmountsByPurchaseIds(purchaseIds)
    )
  }

  fun record(purchaseBulkPaymentCreateDto: PurchaseBulkPaymentCreateDto): List<PurchaseBulkPaymentResponseDto> {
    val purchaseIds = purchaseBulkPaymentCreateDto.purchaseIds
    if (purchaseIds.isEmpty()) throw RtsGenericException("At least one purchase must be selected")
    if (purchaseIds.size > MAX_PAID_PURCHASES) {
      throw RtsGenericException("At most $MAX_PAID_PURCHASES purchases can be paid in one batch")
    }

    purchaseIds.sorted().forEach { purchaseDataFetcher.lockPurchase(it) }
    val purchaseBulkPaymentPlan = purchaseBulkPaymentPlanner.plan(purchaseBulkPaymentCreateDto)
    val supplierPaymentResponseDtos = supplierPaymentService.recordPaymentsForLockedPurchases(
      purchaseBulkPaymentPlan.plannedPayments.map { plannedPayment ->
        SupplierPaymentCreateDto(
          purchaseId = plannedPayment.purchaseId,
          deliveryId = null,
          paymentMethodId = purchaseBulkPaymentCreateDto.paymentMethodId,
          amount = PositiveMoney.of(plannedPayment.amount),
          paymentDate = purchaseBulkPaymentCreateDto.paymentDate,
          notes = purchaseBulkPaymentCreateDto.notes
        )
      },
      purchaseBulkPaymentPlan.ceilingsByPurchaseId,
      purchaseBulkPaymentPlan.alreadyPaidByPurchaseId,
      purchaseBulkPaymentPlan.purchaseInfosByPurchaseId
    )

    val newSupplierPaymentsByPurchaseId = supplierPaymentResponseDtos.associateBy { it.purchaseId }
    return PurchaseBulkPaymentAssembler.buildResponses(
      purchaseBulkPaymentPlan.purchaseEntities,
      purchaseBulkPaymentPlan.ceilingsByPurchaseId,
      purchaseBulkPaymentPlan.alreadyPaidByPurchaseId.mapValues { (purchaseId, alreadyPaid) ->
        alreadyPaid + (newSupplierPaymentsByPurchaseId[purchaseId]?.amount ?: BigDecimal.ZERO)
      },
      newSupplierPaymentsByPurchaseId
    )
  }
}
