package app.myliberty.easy_records.locations.business.purchase.api

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.purchase.PurchaseRepository
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseUpdater(
    private val purchaseRepository: PurchaseRepository
) {

    fun updatePaymentStatus(purchaseId: UUID, status: PaymentStatus) {
        updatePaymentStatuses(mapOf(purchaseId to status))
    }

    fun updatePaymentStatuses(paymentStatusesByPurchaseId: Map<UUID, PaymentStatus>) {
        val purchases = purchaseRepository.findAllById(paymentStatusesByPurchaseId.keys)
        if (purchases.size != paymentStatusesByPurchaseId.size) throw UpdatingNonExistingRecordException()
        purchases.forEach { it.paymentStatus = paymentStatusesByPurchaseId.getValue(it.id!!) }
        purchaseRepository.saveAll(purchases)
    }

    fun updateNotes(id: UUID, notes: Optional<String>?) {
        notes?.let {
            val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
            purchase.notes = StringUtils.getValueOrNull(it)
            purchaseRepository.save(purchase)
        }
    }
}
