package app.myliberty.easy_records.organizations.business.account.api


data class AccountRootCreateRequest(
    val name: String,
    val accountType: AccountType
)
