package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class AccountUsagesFinder(
    private val referenceProviders: List<AccountUsageProvider>
) {

    fun findUsagesForAccountCode(code: String): List<AccountUsageDto> {
        return referenceProviders
            .map { provider -> AccountUsageDto(provider.usageType, provider.getReferences(code)) }
            .filter { it.references.isNotEmpty() }
    }

    fun failOnUsagesForCode(code: String, blockedAction: String) {
        val references = findUsagesForAccountCode(code)
        if (references.isNotEmpty()) {
            throw RtsGenericException("Account is in use and cannot $blockedAction", references)
        }
    }
}
