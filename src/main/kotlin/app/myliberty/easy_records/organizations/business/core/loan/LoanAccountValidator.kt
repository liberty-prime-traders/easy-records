package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionService
import org.springframework.stereotype.Component

@Component
class LoanAccountValidator(
    private val accountSelectionService: AccountSelectionService
) {

    fun requireSelectable(assetAccountCode: String) {
        accountSelectionService.requireSelectable(AccountSelectionRule.LOAN, assetAccountCode)
    }
}
