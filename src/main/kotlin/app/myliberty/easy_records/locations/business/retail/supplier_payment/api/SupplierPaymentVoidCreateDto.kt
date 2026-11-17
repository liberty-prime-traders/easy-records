package app.myliberty.easy_records.locations.business.supplier_payment.api

import java.util.UUID

data class SupplierPaymentVoidCreateDto(
    val supplierPaymentId: UUID,
    val reason: String
)
