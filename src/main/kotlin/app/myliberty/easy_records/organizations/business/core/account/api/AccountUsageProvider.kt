package app.myliberty.easy_records.organizations.business.account.api

interface AccountUsageProvider {
    val usageType: AccountUsageType
    fun getReferences(accountCode: String): List<String>
}
