package app.myliberty.easy_records.organizations.business.core.expense_type

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionService
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeInsertDto
import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseTypeValidator(
    private val accountSelectionService: AccountSelectionService,
    private val expenseTypeRepository: ExpenseTypeRepository,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun guardInsertable(expenseTypeInsertDto: ExpenseTypeInsertDto) {
        guardNameAvailable(expenseTypeInsertDto.name, null)
        guardEligibilityNotEmpty(expenseTypeInsertDto.eligiblePayeeTypes, expenseTypeInsertDto.eligibleSourceTypes)
        guardAccountSelectable(expenseTypeInsertDto.expenseAccountCode)
    }

    fun guardAccountSelectable(expenseAccountCode: String) {
        accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, expenseAccountCode)
    }

    fun guardNameAvailable(name: String, expenseTypeId: UUID?) {
        val requiredName = StringUtils.getValueOrException(name, "Expense type name is required")
        // The name check reads every row and no DB constraint can express isEquivalent, so creates and renames serialize here.
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE_TYPE, EXPENSE_TYPE_NAME_LOCK_KEY)
        expenseTypeRepository.findAll()
            .find { StringUtils.isEquivalent(it.name, requiredName) && it.id != expenseTypeId }
            ?.let { throw LibertyGenericException("An expense type named '$requiredName' already exists") }
    }

    fun guardEligibilityNotEmpty(eligiblePayeeTypes: Set<ContactType>, eligibleSourceTypes: Set<ExpenseSourceType>) {
        if (eligiblePayeeTypes.isEmpty()) throw LibertyGenericException("At least one eligible payee type is required")
        if (eligibleSourceTypes.isEmpty()) throw LibertyGenericException("At least one eligible entry point is required")
    }

    private companion object {
        const val EXPENSE_TYPE_NAME_LOCK_KEY = "name"
    }
}
