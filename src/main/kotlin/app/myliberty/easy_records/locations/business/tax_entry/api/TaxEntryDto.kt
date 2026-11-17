package app.myliberty.easy_records.locations.business.retail.tax_entry.api

import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import java.math.BigDecimal
import java.util.UUID

data class TaxEntryDto(
    val sourceReferenceNumber: String,
    val sourceType: TaxSourceType,
    val direction: TaxDirection,
    val taxTypeId: UUID,
    val taxTypeName: String,
    val jurisdictionName: String,
    val fiscalPeriodId: UUID,
    val calculationMethod: TaxCalculationMethod,
    val rate: BigDecimal,
    val taxIsBilledToCustomerSeparately: Boolean,
    val taxIsIncludedInTaxableAmount: Boolean,
    val taxableAmount: BigDecimal,
    val resolvedTaxableBase: BigDecimal,
    val taxAmount: BigDecimal,
)
