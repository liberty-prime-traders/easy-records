package app.myliberty.easy_records.organizations.business.account.api


data class AccountNameAndTypeDto(
    val code: String,
    val name: String,
    val accountType: AccountType
)
