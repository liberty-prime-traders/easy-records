package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import java.math.BigDecimal

data class ActiveTaxRateDto(
    val ratePercentage: BigDecimal?,
    val rateFlatAmount: BigDecimal?,
    val taxIsBilledToCustomerSeparately: Boolean,
    val taxIsIncludedInTaxableAmount: Boolean
)
