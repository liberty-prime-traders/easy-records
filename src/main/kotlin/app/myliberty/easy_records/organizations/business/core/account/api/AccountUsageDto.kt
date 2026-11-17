package app.myliberty.easy_records.organizations.business.core.account.api

data class AccountUsageDto(
    val usageType: AccountUsageType,
    val references: List<String>
)
