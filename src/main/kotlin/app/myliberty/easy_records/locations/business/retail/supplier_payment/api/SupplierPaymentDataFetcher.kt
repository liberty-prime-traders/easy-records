package app.myliberty.easy_records.locations.business.retail.supplier_payment.api

import app.myliberty.easy_records.locations.business.retail.supplier_payment.PaymentsCalculatorService
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.supplier_payment.SupplierPaymentAssembler
import app.myliberty.easy_records.locations.business.retail.supplier_payment.SupplierPaymentRepository
import app.myliberty.easy_records.locations.business.retail.supplier_payment.SupplierPaymentVoidRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SupplierPaymentDataFetcher(
    private val paymentsCalculatorService: PaymentsCalculatorService,
    private val assembler: SupplierPaymentAssembler,
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val supplierPaymentVoidRepository: SupplierPaymentVoidRepository
) {

    fun getPaidAmountsByPurchaseIds(purchaseIds: Set<UUID>): Map<UUID, BigDecimal> =
        paymentsCalculatorService.calculatePaidAmountsForPurchases(purchaseIds)

    fun getPaymentsByPurchaseId(purchaseId: UUID): List<SupplierPaymentResponseDto> {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        if (payments.isEmpty()) return emptyList()
        val voidsByPaymentId = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .associateBy { it.supplierPaymentId }
        return assembler.buildResponses(payments, voidsByPaymentId)
    }
}
