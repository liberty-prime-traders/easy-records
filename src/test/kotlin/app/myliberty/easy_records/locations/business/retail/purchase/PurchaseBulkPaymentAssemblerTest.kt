package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentResponseDto
import app.myliberty.easy_records.util.enums.PaymentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PurchaseBulkPaymentAssemblerTest {

    private val dateOrdered = OffsetDateTime.of(2026, 1, 5, 9, 0, 0, 0, ZoneOffset.UTC)
    private val purchaseId = UUID.randomUUID()
    private val purchaseEntity = PurchaseEntity(
        supplierId = UUID.randomUUID(),
        purchaseStatus = PurchaseStatus.PARTIALLY_DELIVERED,
        paymentStatus = PaymentStatus.PARTIALLY_SETTLED,
        dateOrdered = dateOrdered
    ).also {
        it.id = purchaseId
        it.referenceNumber = "PRCH-1"
    }
    private val ceilingsByPurchaseId = mapOf(
        purchaseId to PaymentCeiling(BigDecimal("90.00"), false, BigDecimal("40.00"), BigDecimal("80.00"))
    )
    private val paidTotalsByPurchaseId = mapOf(purchaseId to BigDecimal("25.00"))

    @Test
    fun `a purchase without a new payment keeps its stored payment status and applies nothing`() {
        val response = PurchaseBulkPaymentAssembler
            .buildResponses(listOf(purchaseEntity), ceilingsByPurchaseId, paidTotalsByPurchaseId)
            .single()

        assertEquals(purchaseId, response.id)
        assertEquals("PRCH-1", response.referenceNumber)
        assertEquals(PurchaseStatus.PARTIALLY_DELIVERED, response.purchaseStatus)
        assertEquals(dateOrdered, response.dateOrdered)
        assertEquals(BigDecimal("80.00"), response.orderedTotal)
        assertEquals(BigDecimal("90.00"), response.paymentCeiling)
        assertEquals(BigDecimal("25.00"), response.paidTotal)
        assertEquals(0, BigDecimal("65.00").compareTo(response.payable))
        assertEquals(PaymentStatus.PARTIALLY_SETTLED, response.paymentStatus)
        assertEquals(0, response.appliedAmount.signum())
        assertNull(response.supplierPaymentId)
        assertNull(response.paymentReferenceNumber)
    }

    @Test
    fun `a purchase with a new payment shows the payment and the status it produced`() {
        val supplierPaymentId = UUID.randomUUID()
        val newPayment = SupplierPaymentResponseDto(
            id = supplierPaymentId, referenceNumber = "SUP-9", purchaseId = purchaseId, purchaseReferenceNumber = "PRCH-1",
            paymentMethod = "", supplier = "", amount = BigDecimal("65.00"), paymentDate = dateOrdered, notes = null,
            createdBy = "", createdOn = "", updatedPurchasePaymentStatus = PaymentStatus.FULLY_SETTLED
        )

        val response = PurchaseBulkPaymentAssembler
            .buildResponses(
                listOf(purchaseEntity), ceilingsByPurchaseId, paidTotalsByPurchaseId, mapOf(purchaseId to newPayment)
            )
            .single()

        assertEquals(PaymentStatus.FULLY_SETTLED, response.paymentStatus)
        assertEquals(BigDecimal("65.00"), response.appliedAmount)
        assertEquals(supplierPaymentId, response.supplierPaymentId)
        assertEquals("SUP-9", response.paymentReferenceNumber)
    }
}
