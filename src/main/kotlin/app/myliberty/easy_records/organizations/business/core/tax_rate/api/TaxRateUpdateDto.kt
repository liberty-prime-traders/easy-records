package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateDto
import java.io.Serializable
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

data class TaxRateUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val endDate: Optional<LocalDate>? = null
) : Serializable {

    fun applyTo(existing: TaxRateDto): TaxRateDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        endDate = endDate?.orElse(existing.endDate) ?: existing.endDate
    )
}
