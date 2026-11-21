package app.myliberty.easy_records.locations.business.retail.sale_payment

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentResponseDto
import java.util.UUID

object SalePaymentMapper {

    fun toResponseDto(
        payment: SalePaymentEntity,
        voidedReason: String?,
        paymentMethodNamesById: Map<UUID, String> = emptyMap(),
        updatedStatus: PaymentStatus? = null,
        updatedSaleVersion: Long? = null,
    ) = SalePaymentResponseDto(
        id = payment.id!!,
        referenceNumber = payment.requiredReference(),
        saleId = payment.saleId,
        amount = payment.amount,
        reference = payment.reference,
        paymentDate = payment.paymentDate,
        paymentMethodName = paymentMethodNamesById.getValue(payment.paymentMethodId),
        voidedReason = voidedReason,
        updatedSalePaymentStatus = updatedStatus,
        updatedSaleVersion = updatedSaleVersion,
    )
}
