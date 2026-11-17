package app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type

import app.myliberty.easy_records.organizations.business.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.organizations.business.account_selection.api.AccountSelectionService
import app.myliberty.easy_records.platform.business.tax_type.api.PlatformTaxTypeDto
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class TaxAccountsValidator(private val accountSelectionService: AccountSelectionService) {

    fun validate(
        payableAccountCode: String,
        recoverableAccountCode: String?,
        platformTaxType: PlatformTaxTypeDto
    ) {
        val payableCode = StringUtils.getValueOrException(payableAccountCode, "Every tax type must be associated with a payable account")
        accountSelectionService.requireSelectable(AccountSelectionRule.TAX_PAYABLE, payableCode)
        validateRecoverableAccountCode(recoverableAccountCode, platformTaxType)
    }

    private fun validateRecoverableAccountCode(code: String?, platformTaxType: PlatformTaxTypeDto) {
        val normalizedCode = StringUtils.getValueOrNull(code)
        val taxLabel = platformTaxType.label
        if (platformTaxType.taxRecoveryType == TaxRecoveryType.RECOVERABLE) {
            if (normalizedCode == null) {
                throw RtsGenericException("$taxLabel requires a recoverable account")
            }
        } else {
            if (normalizedCode != null) {
                throw RtsGenericException("$taxLabel is not recoverable, so a recoverable account should not be provided")
            }
            return
        }
        accountSelectionService.requireSelectable(AccountSelectionRule.TAX_RECOVERABLE, normalizedCode)
    }
}
