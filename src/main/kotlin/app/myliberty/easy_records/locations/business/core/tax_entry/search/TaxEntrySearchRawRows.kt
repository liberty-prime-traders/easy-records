package app.myliberty.easy_records.locations.business.core.tax_entry.search

import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxDirection
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxSourceType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TaxEntrySearchRawRow(
    val id: UUID,
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
    val createdOn: OffsetDateTime
)

data class TaxEntrySourceTypeSummaryRawRow(
    val fiscalPeriodId: UUID,
    val taxTypeId: UUID,
    val taxTypeName: String,
    val jurisdictionName: String,
    val sourceType: TaxSourceType,
    val entryCount: Long,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal
)
