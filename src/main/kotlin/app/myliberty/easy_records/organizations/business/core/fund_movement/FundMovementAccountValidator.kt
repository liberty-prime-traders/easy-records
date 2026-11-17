package app.myliberty.easy_records.organizations.business.fund_movement

import app.myliberty.easy_records.organizations.business.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.organizations.business.account_selection.api.AccountSelectionService
import org.springframework.stereotype.Component

@Component
class FundMovementAccountValidator(
    private val accountSelectionService: AccountSelectionService
) {

    // Sorted so two movements over the same pair of accounts take the account locks in the same order.
    fun requireSelectable(userPickedAccountCodes: Collection<String>) {
        userPickedAccountCodes.distinct().sorted().forEach { accountCode ->
            accountSelectionService.requireSelectable(AccountSelectionRule.FUND_MOVEMENT, accountCode)
        }
    }
}
