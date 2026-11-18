package app.myliberty.easy_records.locations.business.retail.supplier_payment

import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentVoidDto(
    val id: UUID,
    val referenceNumber: String,
    val supplierPaymentId: UUID,
    val reason: String,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
