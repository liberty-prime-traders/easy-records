package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentResponseDto
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentService
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentVoidCreateDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/sale-payments")
class SalePaymentEndpoint(
    private val salePaymentService: SalePaymentService,
) {

    @PostMapping("void")
    fun voidPayment(@RequestBody dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        return salePaymentService.voidPayment(dto)
    }
}
