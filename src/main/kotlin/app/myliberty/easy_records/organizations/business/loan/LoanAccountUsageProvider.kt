package app.myliberty.easy_records.organizations.business.loan

import app.myliberty.easy_records.organizations.business.account.api.AccountUsageProvider
import app.myliberty.easy_records.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class LoanAccountUsageProvider(
    private val loanRepository: LoanRepository,
    private val loanRepaymentRepository: LoanRepaymentRepository
) : AccountUsageProvider {

    override val usageType = AccountUsageType.LOAN

    override fun getReferences(accountCode: String): List<String> {
        val loanReferences = loanRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc(accountCode)
            .map { it.requiredReference() }
        val repaymentReferences = loanRepaymentRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc(accountCode)
            .map { it.requiredReference() }
        return loanReferences + repaymentReferences
    }
}
