package app.myliberty.easy_records.locations.rest.endpoints.retail

import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentCreateDto
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentDataFetcher
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentResponseDto
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentService
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.SupplierPaymentVoidCreateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/supplier-payments")
class SupplierPaymentEndpoint(
    private val supplierPaymentService: SupplierPaymentService,
    private val supplierPaymentDataFetcher: SupplierPaymentDataFetcher
) {

    @PostMapping
    fun recordPayment(@RequestBody dto: SupplierPaymentCreateDto): SupplierPaymentResponseDto =
        supplierPaymentService.recordPayment(dto)

    @PostMapping("void")
    fun voidPayment(@RequestBody dto: SupplierPaymentVoidCreateDto): SupplierPaymentResponseDto =
        supplierPaymentService.voidPayment(dto)

    @GetMapping
    fun getByPurchaseId(@RequestParam purchaseId: UUID): List<SupplierPaymentResponseDto> =
        supplierPaymentDataFetcher.getPaymentsByPurchaseId(purchaseId)
}
