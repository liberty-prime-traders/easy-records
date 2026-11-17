package app.myliberty.easy_records.organizations.business.core.fund_movement

import app.myliberty.easy_records.organizations.business.core.account.api.AccountUsageProvider
import app.myliberty.easy_records.organizations.business.core.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class FundMovementAccountUsageProvider(
    private val fundMovementRepository: FundMovementRepository
) : AccountUsageProvider {

    override val usageType = AccountUsageType.FUND_MOVEMENT

    override fun getReferences(accountCode: String): List<String> {
        return fundMovementRepository
            .findTop10ByDebitAccountCodeOrCreditAccountCodeOrderByCreatedOnDesc(accountCode, accountCode)
            .map { it.requiredReference() }
    }
}
