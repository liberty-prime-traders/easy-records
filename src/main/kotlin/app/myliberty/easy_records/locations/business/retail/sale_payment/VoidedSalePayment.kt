package app.myliberty.easy_records.locations.business.retail.sale_payment

data class VoidedSalePayment(
    val payment: SalePaymentEntity,
    val voidEntity: SalePaymentVoidEntity,
)
