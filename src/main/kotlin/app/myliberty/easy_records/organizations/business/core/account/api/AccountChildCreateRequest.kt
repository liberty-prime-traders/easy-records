package app.myliberty.easy_records.organizations.business.account.api

data class AccountChildCreateRequest(
    val parentAccountCode: String,
    val name: String
)
