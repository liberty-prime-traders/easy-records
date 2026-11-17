package app.myliberty.easy_records.organizations.business.opening_balance.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.opening_balance.OpeningBalanceRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OpeningBalanceReader(
    private val openingBalanceRepository: OpeningBalanceRepository
) {

    fun getAmountsByAccountCodes(accountCodes: Set<String>): Map<String, BigDecimal> {
        if (accountCodes.isEmpty()) return emptyMap()
        return openingBalanceRepository.findLatestForAccountCodes(accountCodes)
            .associate { it.accountCode to it.amount }
    }
}
