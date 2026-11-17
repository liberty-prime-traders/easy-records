package app.myliberty.easy_records.locations.business.retail.tax_entry.api

import java.math.BigDecimal
import java.util.UUID

data class TaxTypePeriodSummaryDto(
    val fiscalPeriodId: UUID,
    val fiscalPeriodName: String,
    val taxTypeId: UUID,
    val taxTypeName: String,
    val jurisdictionName: String,
    val entryCount: Long,
    val grossTaxable: BigDecimal,
    val grossTax: BigDecimal,
    val reversalTax: BigDecimal,
    val netTax: BigDecimal
)

data class TaxEntrySearchSummaryResponseDto(
    val groups: List<TaxTypePeriodSummaryDto>,
    val grossTax: BigDecimal,
    val reversalTax: BigDecimal,
    val netTax: BigDecimal,
    val entryCount: Long
)
