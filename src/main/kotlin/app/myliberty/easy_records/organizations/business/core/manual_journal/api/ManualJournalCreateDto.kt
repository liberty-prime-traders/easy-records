package app.myliberty.easy_records.organizations.business.core.manual_journal.api

import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.LocalDate

data class ManualJournalCreateDto(
    val summary: String,
    val details: String? = null,
    val postingDate: LocalDate? = null,
    val lines: List<ManualJournalLineCreateDto>
)

data class ManualJournalLineCreateDto(
    val accountCode: String,
    val entryType: EntryType,
    val amount: PositiveMoney,
    val lineMemo: String? = null
)
