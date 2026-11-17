package app.myliberty.easy_records.locations.business.retail.sale_payment.api

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale_payment.SalePaymentWriter
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SalePaymentAppender(
    private val salePaymentWriter: SalePaymentWriter,
) {

    fun appendNew(
        saleId: UUID,
        contactId: UUID,
        receivableTotal: BigDecimal,
        saleSaveRequest: SaleSaveRequest,
    ): AppendResult {
        val newSalePaymentSaveRequests = saleSaveRequest.salePayments.filter { it.existingId == null }
        val newSalePayments = newSalePaymentSaveRequests.map { salePaymentSaveRequest ->
            SalePaymentWriter.NewSalePayment(
                paymentMethodId = salePaymentSaveRequest.paymentMethodId,
                amount = salePaymentSaveRequest.amount,
                reference = salePaymentSaveRequest.reference,
                paymentDate = salePaymentSaveRequest.paymentDate,
            )
        }
        val writeResult = salePaymentWriter.write(saleId, contactId, receivableTotal, newSalePayments)
        val persistedSalePaymentsByClientKey = newSalePaymentSaveRequests
            .mapIndexed { paymentIndex, salePaymentSaveRequest ->
                val savedSalePayment = writeResult.savedSalePayments[paymentIndex]
                salePaymentSaveRequest.clientKey to PersistedSalePayment(
                    id = savedSalePayment.id!!,
                    paymentDate = savedSalePayment.paymentDate,
                )
            }.toMap()
        return AppendResult(
            persistedSalePaymentsByClientKey = persistedSalePaymentsByClientKey,
            newPaymentStatus = writeResult.newPaymentStatus,
        )
    }

    data class AppendResult(
        val persistedSalePaymentsByClientKey: Map<UUID, PersistedSalePayment>,
        val newPaymentStatus: PaymentStatus,
    )
}
