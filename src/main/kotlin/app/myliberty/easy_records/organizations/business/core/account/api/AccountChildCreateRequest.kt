package app.myliberty.easy_records.organizations.business.core.account.api

data class AccountChildCreateRequest(
    val parentAccountCode: String,
    val name: String
)
