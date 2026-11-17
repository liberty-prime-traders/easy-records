package app.myliberty.easy_records.organizations.business.account.api

import java.math.BigDecimal

data class AccountPosting(
    val accountCode: String,
    val amount: BigDecimal,
    val entryType: EntryType
)
