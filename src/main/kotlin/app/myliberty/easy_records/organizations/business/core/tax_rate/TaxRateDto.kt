package app.myliberty.easy_records.organizations.business.core.tax_rate

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TaxRateDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val orgJurisdictionTaxTypeId: UUID,
    val name: String,
    val ratePercentage: BigDecimal? = null,
    val rateFlatAmount: BigDecimal? = null,
    val taxIsBilledToCustomerSeparately: Boolean,
    val taxIsIncludedInTaxableAmount: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)
