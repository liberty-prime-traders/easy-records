package app.myliberty.easy_records.organizations.business.core.account.api


data class AccountRootCreateRequest(
    val name: String,
    val accountType: AccountType
)
