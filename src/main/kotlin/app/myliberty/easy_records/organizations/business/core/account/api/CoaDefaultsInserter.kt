package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.org_profile.api.OrgDataSeeder
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
@Order(OrgDataSeeder.CHART_OF_ACCOUNTS)
class CoaDefaultsInserter(
    private val accountCache: AccountCache,
    private val accountStructureLock: AccountStructureLock
) : OrgDataSeeder {

    override fun seed() {
        accountStructureLock.acquire(SystemAccount.entries.mapNotNull { it.parent?.code }.toSet())
        val allAccounts = accountCache.getAllFresh().map { it.code }.toSet()
        val toInsert = SystemAccount.entries.filter { it.code !in allAccounts }
            .map { buildInsertDto(it) }
        if (toInsert.isNotEmpty()) {
            accountCache.saveAll(toInsert)
        }
    }

    private fun buildInsertDto(account: SystemAccount): AccountInsertDto {
        return AccountInsertDto(
            code = account.code,
            name = account.accountName,
            accountType = account.type,
            accountIsSystemMaintained = true,
            parentAccountCode = account.parent?.code
        )
    }
}
