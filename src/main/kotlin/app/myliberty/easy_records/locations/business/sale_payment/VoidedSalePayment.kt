package me.ezra_home.retail_software_solution.locations.business.sale_payment

data class VoidedSalePayment(
    val payment: SalePaymentEntity,
    val voidEntity: SalePaymentVoidEntity,
)
