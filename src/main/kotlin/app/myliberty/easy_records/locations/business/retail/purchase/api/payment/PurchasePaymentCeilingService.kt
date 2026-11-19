package app.myliberty.easy_records.locations.business.retail.purchase.api.payment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineEntity
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchasePaymentCeilingService(
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher
) {

    data class PaymentCeiling(
        val amount: BigDecimal,
        val isFullyDelivered: Boolean,
        val deliveredTotal: BigDecimal,
        val orderedTotal: BigDecimal
    )

    fun computeCeiling(purchaseId: UUID): PaymentCeiling = computeCeilings(setOf(purchaseId)).getValue(purchaseId)

    fun computeCeilings(purchaseIds: Set<UUID>): Map<UUID, PaymentCeiling> {
        val linesByPurchaseId = purchaseLineRepository.findByPurchaseIdIn(purchaseIds).groupBy { it.purchaseId }
        val deliveredTotalsByPurchaseId = purchaseDeliveryDataFetcher.calculateDeliveredTotals(purchaseIds)
        return purchaseIds.associateWith { purchaseId ->
            computeCeiling(linesByPurchaseId[purchaseId].orEmpty(), deliveredTotalsByPurchaseId.getValue(purchaseId))
        }
    }

    fun computeCeiling(lines: List<PurchaseLineEntity>, deliveredTotal: BigDecimal): PaymentCeiling {
        val poTotal = lines.sumOf { it.getTotalCost() }
        val isFullyDelivered = deliveredTotal > BigDecimal.ZERO &&
            lines.none { it.storageQuantityRemaining().isPositive() }
        val ceiling = when {
            isFullyDelivered -> deliveredTotal
            deliveredTotal > BigDecimal.ZERO -> poTotal.max(deliveredTotal)
            else -> poTotal
        }
        return PaymentCeiling(
            amount = ceiling,
            isFullyDelivered = isFullyDelivered,
            deliveredTotal = deliveredTotal,
            orderedTotal = poTotal
        )
    }
}
