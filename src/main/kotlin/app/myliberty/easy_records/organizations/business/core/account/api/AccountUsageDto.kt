package app.myliberty.easy_records.organizations.business.account.api

data class AccountUsageDto(
    val usageType: AccountUsageType,
    val references: List<String>
)
