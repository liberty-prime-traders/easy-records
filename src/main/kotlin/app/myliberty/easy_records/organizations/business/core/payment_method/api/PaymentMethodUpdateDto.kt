package app.myliberty.easy_records.organizations.business.core.payment_method.api

import app.myliberty.easy_records.organizations.business.core.payment_method.PaymentMethodDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class PaymentMethodUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val accountCode: Optional<String>? = null
): Serializable {

    fun applyTo(existing: PaymentMethodDto): PaymentMethodDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = StringUtils.useIfProvided(description, existing.description),
        accountCode = StringUtils.useIfProvided(accountCode, existing.accountCode)
            ?: throw LibertyGenericException("A Payment Method must have an account")
    )
}
