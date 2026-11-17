package app.myliberty.easy_records.organizations.business.expense_type

import app.myliberty.easy_records.organizations.business.account.api.AccountUsageProvider
import app.myliberty.easy_records.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class ExpenseTypeAccountUsageProvider(
    private val expenseTypeRepository: ExpenseTypeRepository
) : AccountUsageProvider {

    override val usageType = AccountUsageType.EXPENSE_TYPE

    override fun getReferences(accountCode: String): List<String> {
        return expenseTypeRepository.findAllByExpenseAccountCode(accountCode)
            .map { it.name }
    }
}
