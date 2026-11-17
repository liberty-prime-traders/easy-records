package app.myliberty.easy_records.locations.business.sale_payment

data class VoidedSalePayment(
    val payment: SalePaymentEntity,
    val voidEntity: SalePaymentVoidEntity,
)
