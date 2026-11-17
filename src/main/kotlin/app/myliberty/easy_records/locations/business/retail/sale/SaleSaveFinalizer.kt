package app.myliberty.easy_records.locations.business.sale

import app.myliberty.easy_records.locations.business.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.sale_adjustment.api.RetainedSaleAdjustments
import app.myliberty.easy_records.locations.business.sale_adjustment.api.SaleAdjustmentSyncer
import app.myliberty.easy_records.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import app.myliberty.easy_records.locations.business.sale_payment.api.PersistedSalePayment
import app.myliberty.easy_records.locations.business.sale_payment.api.SalePaymentAppender
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSaveFinalizer(
    private val saleRepository: SaleRepository,
    private val saleAdjustmentSyncer: SaleAdjustmentSyncer,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val saleTotalsApplier: SaleTotalsApplier,
    private val salePaymentAppender: SalePaymentAppender,
) {

    fun finalize(
        saleEntity: SaleEntity,
        saleSaveRequest: SaleSaveRequest,
        lineSyncResult: SaleLineSync.Result,
        retainedSaleAdjustments: RetainedSaleAdjustments,
    ): SaleSaveFinalizeResult {
        val saleId = saleEntity.id!!
        val saleAdjustmentIdsByClientKey = saleAdjustmentSyncer.sync(
            saleId = saleId,
            saleSaveRequest = saleSaveRequest,
            persistedSaleLines = lineSyncResult.toPersistedSaleLines(),
            saleLineIdsByClientKey = lineSyncResult.saleLineIdsByClientKey,
            retainedSaleAdjustments = retainedSaleAdjustments,
        )
        saleTotalsApplier.applyTotals(
            saleEntity,
            lineSyncResult.persistedSaleLines,
            saleAdjustmentFetcher.getAdjustmentSummaries(saleId),
        )
        val salePaymentAppendResult = salePaymentAppender.appendNew(
            saleId = saleId,
            contactId = saleEntity.contactId,
            receivableTotal = saleEntity.receivableTotal(),
            saleSaveRequest = saleSaveRequest,
        )
        saleEntity.paymentStatus = salePaymentAppendResult.newPaymentStatus
        saleRepository.saveAndFlush(saleEntity)
        return SaleSaveFinalizeResult(
            saleAdjustmentIdsByClientKey = saleAdjustmentIdsByClientKey,
            persistedSalePaymentsByClientKey = salePaymentAppendResult.persistedSalePaymentsByClientKey,
        )
    }

    data class SaleSaveFinalizeResult(
        val saleAdjustmentIdsByClientKey: Map<UUID, UUID>,
        val persistedSalePaymentsByClientKey: Map<UUID, PersistedSalePayment>,
    )
}
