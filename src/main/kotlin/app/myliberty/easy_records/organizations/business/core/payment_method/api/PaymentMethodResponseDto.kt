package app.myliberty.easy_records.organizations.business.core.payment_method.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentMethodResponseDto(
    val id: UUID,
    val createdBy: String,
    val createdOn: OffsetDateTime,
    val name: String,
    val description: String?,
    val referenceNumber: String,
    val accountCode: String,
    val linkedAccount: String?
): Serializable
