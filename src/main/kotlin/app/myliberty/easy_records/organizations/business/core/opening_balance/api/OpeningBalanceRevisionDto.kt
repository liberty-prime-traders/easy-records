package app.myliberty.easy_records.organizations.business.core.opening_balance.api

import java.math.BigDecimal
import java.time.OffsetDateTime

data class OpeningBalanceRevisionDto(
    val referenceNumber: String,
    val accountCode: String,
    val amount: BigDecimal,
    val changedBy: String,
    val changedAt: OffsetDateTime
)
