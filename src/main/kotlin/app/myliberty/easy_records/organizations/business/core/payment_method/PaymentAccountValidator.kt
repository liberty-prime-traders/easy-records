package app.myliberty.easy_records.organizations.business.core.payment_method

import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionService
import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.stereotype.Component

@Component
class PaymentAccountValidator(private val accountSelectionService: AccountSelectionService) {

    fun validate(accountCode: String) {
        val code = StringUtils.getValueOrException(accountCode, "A Payment Method must have an account")
        accountSelectionService.requireSelectable(AccountSelectionRule.PAYMENT_METHOD, code)
    }
}
