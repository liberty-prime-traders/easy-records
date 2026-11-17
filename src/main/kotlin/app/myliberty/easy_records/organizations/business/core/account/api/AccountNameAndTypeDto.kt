package app.myliberty.easy_records.organizations.business.core.account.api


data class AccountNameAndTypeDto(
    val code: String,
    val name: String,
    val accountType: AccountType
)
