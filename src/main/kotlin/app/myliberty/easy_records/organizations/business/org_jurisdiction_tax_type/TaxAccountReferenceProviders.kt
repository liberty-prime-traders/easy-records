package app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type

import app.myliberty.easy_records.organizations.business.account.api.AccountUsageProvider
import app.myliberty.easy_records.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class TaxPayableAccountUsageProvider(
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache
) : AccountUsageProvider {

    override val usageType = AccountUsageType.TAX_PAYABLE

    override fun getReferences(accountCode: String): List<String> {
        return orgJurisdictionTaxTypeCache.getAll()
            .filter { it.payableAccountCode == accountCode }
            .map { it.referenceNumber }
    }
}

@Component
class TaxRecoverableAccountUsageProvider(
    private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache
) : AccountUsageProvider {

    override val usageType = AccountUsageType.TAX_RECOVERABLE

    override fun getReferences(accountCode: String): List<String> {
        return orgJurisdictionTaxTypeCache.getAll()
            .filter { it.recoverableAccountCode == accountCode }
            .map { it.referenceNumber }
    }
}
