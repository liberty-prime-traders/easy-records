package app.myliberty.easy_records.organizations.business.ledger.api

import app.myliberty.easy_records.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.LocalDate

data class LedgerMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val source: LedgerSourceType,
    val amount: BigDecimal
)

data class LedgerAccountMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val amount: BigDecimal
)
