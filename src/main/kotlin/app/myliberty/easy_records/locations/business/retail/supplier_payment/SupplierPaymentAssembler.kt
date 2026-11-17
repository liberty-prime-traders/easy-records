package app.myliberty.easy_records.locations.business.supplier_payment

import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseDataFetcher.PurchaseInfo
import app.myliberty.easy_records.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SupplierPaymentAssembler(
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val contactService: ContactService,
    private val userQualifier: UserQualifier
) {

    fun buildResponse(
        payment: SupplierPaymentEntity,
        paymentVoid: SupplierPaymentVoidEntity?,
        paymentStatus: PaymentStatus?
    ): SupplierPaymentResponseDto {
        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoById(payment.purchaseId)
        val deliveryRef = payment.deliveryId?.let {
            purchaseDeliveryDataFetcher.getDeliveryReferenceNumbersById(listOf(it))[it]
        }
        val paymentMethodName = paymentMethodDataFetcher.getNamesById()[payment.paymentMethodId] ?: ""
        val supplierName = contactService.getContactById(purchaseInfo.supplierId).identity.displayName
        return toDto(
            payment, purchaseInfo.referenceNumber,
            deliveryRef, paymentMethodName, supplierName,
            paymentVoid, paymentStatus
        )
    }

    fun buildResponses(
        payments: List<SupplierPaymentEntity>,
        voidsByPaymentId: Map<UUID, SupplierPaymentVoidEntity>
    ): List<SupplierPaymentResponseDto> {
        if (payments.isEmpty()) return emptyList()
        return buildResponses(
            payments,
            voidsByPaymentId,
            purchaseDataFetcher.findPurchaseInfoByIds(payments.map { it.purchaseId }.toSet())
        )
    }

    fun buildResponses(
        payments: List<SupplierPaymentEntity>,
        voidsByPaymentId: Map<UUID, SupplierPaymentVoidEntity>,
        purchaseInfoById: Map<UUID, PurchaseInfo>
    ): List<SupplierPaymentResponseDto> {
        if (payments.isEmpty()) return emptyList()
        val deliveryIds = payments.mapNotNull { it.deliveryId }
        val deliveryRefsById = if (deliveryIds.isNotEmpty())
            purchaseDeliveryDataFetcher.getDeliveryReferenceNumbersById(deliveryIds)
        else
            emptyMap()
        val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()
        val supplierNamesById = contactService.getAllContactDtos().associateBy(
            { it.id }, { it.identity.displayName }
        )
        return payments.map { payment ->
            val purchaseInfo = purchaseInfoById.getValue(payment.purchaseId)
            toDto(
                payment,
                purchaseInfo.referenceNumber,
                payment.deliveryId?.let { deliveryRefsById[it] },
                paymentMethodNamesById[payment.paymentMethodId] ?: "",
                supplierNamesById[purchaseInfo.supplierId] ?: "",
                voidsByPaymentId[payment.id!!],
                purchaseInfo.paymentStatus
            )
        }
    }

    private fun toDto(
        payment: SupplierPaymentEntity,
        purchaseRef: String,
        deliveryRef: String?,
        paymentMethodName: String,
        supplierName: String,
        paymentVoid: SupplierPaymentVoidEntity?,
        paymentStatus: PaymentStatus?
    ) = SupplierPaymentResponseDto(
        id = payment.id!!,
        referenceNumber = payment.requiredReference(),
        purchaseId = payment.purchaseId,
        purchaseReferenceNumber = purchaseRef,
        deliveryReferenceNumber = deliveryRef,
        paymentMethod = paymentMethodName,
        supplier = supplierName,
        amount = payment.amount,
        paymentDate = payment.paymentDate,
        notes = payment.notes,
        createdBy = userQualifier.getUserFullName(payment.createdById) ?: "",
        createdOn = payment.createdOn.toString(),
        voidedReason = paymentVoid?.reason,
        updatedPurchasePaymentStatus = paymentStatus
    )
}
