package app.myliberty.easy_records.organizations.business.account.api

data class AccountLookupDto(
    val code: String,
    val accountIsActive: Boolean,
    val hasChildren: Boolean,
    val normalBalanceEntryType: EntryType
)
