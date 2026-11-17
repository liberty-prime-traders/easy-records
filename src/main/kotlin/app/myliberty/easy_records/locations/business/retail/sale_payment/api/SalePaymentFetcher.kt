package app.myliberty.easy_records.locations.business.retail.sale_payment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale_payment.ActiveSalePaymentRepository
import app.myliberty.easy_records.locations.business.retail.sale_payment.SalePaymentRepository
import app.myliberty.easy_records.locations.business.retail.sale_payment.SalePaymentVoidRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentSnapshot(
    val id: UUID,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String?,
)

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentFetcher(
    private val salePaymentRepository: SalePaymentRepository,
    private val activeSalePaymentRepository: ActiveSalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository
) {

    fun calculatePaidAmount(saleId: UUID): BigDecimal {
        return calculatePaidAmounts(listOf(saleId))[saleId] ?: BigDecimal.ZERO
    }

    fun calculatePaidAmounts(saleIds: List<UUID>): Map<UUID, BigDecimal> {
        return activeSalePaymentRepository.findBySaleIdIn(saleIds)
            .groupBy { it.saleId }
            .mapValues { (_, activePayments) -> activePayments.sumOf { it.amount } }
    }


    fun getPaymentSnapshots(saleId: UUID): List<SalePaymentSnapshot> =
        getPaymentSnapshotsBySaleId(listOf(saleId)).getValue(saleId)

    fun getPaymentSnapshotsBySaleId(saleIds: List<UUID>): Map<UUID, List<SalePaymentSnapshot>> {
        val payments = salePaymentRepository.findBySaleIdIn(saleIds)
        val voidReasonsByPaymentId = if (payments.isEmpty()) emptyMap() else salePaymentVoidRepository
            .findBySalePaymentIdIn(payments.map { it.id!! })
            .associate { it.salePaymentId to it.reason }
        val snapshotsBySaleId = payments.groupBy({ it.saleId }, { entity ->
            SalePaymentSnapshot(
                id = entity.id!!,
                paymentMethodId = entity.paymentMethodId,
                amount = entity.amount,
                reference = entity.reference,
                paymentDate = entity.paymentDate,
                voidedReason = voidReasonsByPaymentId[entity.id!!],
            )
        })
        return saleIds.associateWith { snapshotsBySaleId[it].orEmpty() }
    }

    fun saleIdsWithActivePayments(saleIds: List<UUID>): Set<UUID> =
        activeSalePaymentRepository.findSaleIdsWithActivePayments(saleIds)
}
