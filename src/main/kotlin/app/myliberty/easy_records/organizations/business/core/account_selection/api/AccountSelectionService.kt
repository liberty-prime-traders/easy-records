package app.myliberty.easy_records.organizations.business.core.account_selection.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.account.api.AccountDataFetcher
import app.myliberty.easy_records.organizations.business.core.account.api.AccountStructureLock
import app.myliberty.easy_records.organizations.business.core.account_selection.AccountTreeBuilder
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class AccountSelectionService(
    private val accountDataFetcher: AccountDataFetcher,
    private val accountStructureLock: AccountStructureLock
) {

    fun buildTreesForSelection(): AccountsTreesForSelection {
        val candidates = accountDataFetcher.getSelectionCandidates()
        return AccountsTreesForSelection(
            payable = AccountTreeBuilder.build(candidates, AccountSelectionRule.TAX_PAYABLE),
            recoverable = AccountTreeBuilder.build(candidates, AccountSelectionRule.TAX_RECOVERABLE),
            paymentMethods = AccountTreeBuilder.build(candidates, AccountSelectionRule.PAYMENT_METHOD),
            expenseTypes = AccountTreeBuilder.build(candidates, AccountSelectionRule.EXPENSE_TYPE),
            fundMovements = AccountTreeBuilder.build(candidates, AccountSelectionRule.FUND_MOVEMENT),
            loans = AccountTreeBuilder.build(candidates, AccountSelectionRule.LOAN),
            manualJournals = AccountTreeBuilder.build(candidates, AccountSelectionRule.MANUAL_JOURNAL)
        )
    }

    // The lock holds until the caller's transaction ends, so the leaf/active facts checked here cannot change before the
    // caller saves its reference; createChild and toggleActive take the same lock.
    @TransactionalOnOrganizationSchema
    fun requireSelectable(rule: AccountSelectionRule, accountCode: String) {
        accountStructureLock.acquire(accountCode)
        val candidate = accountDataFetcher.getFreshSelectionCandidates().firstOrNull { it.code == accountCode }
            ?: throw RtsGenericException("${rule.accountDescription} not found: $accountCode")
        if (!candidate.accountIsActive) {
            throw RtsGenericException("${rule.accountDescription} is inactive: ${candidate.label}")
        }
        rule.rejectionReason(candidate)?.let { throw RtsGenericException(it) }
    }
}
