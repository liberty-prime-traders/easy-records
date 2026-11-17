package app.myliberty.easy_records.locations.business.purchase

import app.myliberty.easy_records.locations.business.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentSearchDto
import app.myliberty.easy_records.locations.business.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.locations.business.purchase.search.PurchaseSearchFetcher
import app.myliberty.easy_records.locations.business.purchase.search.PurchaseSearchValidator
import app.myliberty.easy_records.util.enums.PaymentStatus
import org.springframework.stereotype.Component

@Component
class PayablePurchaseFinder(private val purchaseSearchFetcher: PurchaseSearchFetcher) {

  companion object {
    val PAYABLE_PURCHASE_STATUSES = listOf(
      PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED, PurchaseStatus.FULLY_DELIVERED
    )
    const val MAX_PAYABLE_RESULTS = 50
    private val PAYABLE_PAYMENT_STATUSES = listOf(PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_SETTLED)
  }

  fun findPayablePurchases(purchaseBulkPaymentSearchDto: PurchaseBulkPaymentSearchDto): List<PurchaseEntity> {
    PurchaseSearchValidator.guardValidPayableRange(
      purchaseBulkPaymentSearchDto.orderedFrom, purchaseBulkPaymentSearchDto.orderedBefore
    )
    val purchaseSearchParameters = PurchaseSearchParameters(
      purchaseDateFrom = purchaseBulkPaymentSearchDto.orderedFrom,
      purchaseDateBefore = purchaseBulkPaymentSearchDto.orderedBefore,
      supplierIds = listOf(purchaseBulkPaymentSearchDto.supplierId),
      purchaseStatuses = PAYABLE_PURCHASE_STATUSES,
      paymentStatuses = PAYABLE_PAYMENT_STATUSES
    )
    return purchaseSearchFetcher.search(purchaseSearchParameters, null, MAX_PAYABLE_RESULTS, oldestFirst = true)
      .take(MAX_PAYABLE_RESULTS)
  }
}
