package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import app.myliberty.easy_records.util.business.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TaxRateInsertDto(
    val orgJurisdictionTaxTypeId: UUID,
    val name: String,
    val ratePercentage: BigDecimal? = null,
    val rateFlatAmount: Money? = null,
    val taxIsBilledToCustomerSeparately: Boolean,
    val taxIsIncludedInTaxableAmount: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)
