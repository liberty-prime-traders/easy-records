package app.myliberty.easy_records.organizations.business.expense_type.api

import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.contact.api.ContactType
import java.util.Optional
import java.util.UUID

data class ExpenseTypeUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val expenseAccountCode: Optional<String>? = null,
    val eligiblePayeeTypes: Optional<Set<ContactType>>? = null,
    val eligibleSourceTypes: Optional<Set<ExpenseSourceType>>? = null
) {
    fun applyTo(existingExpenseTypeDto: ExpenseTypeDto): ExpenseTypeDto = existingExpenseTypeDto.copy(
        name = name?.orElse(existingExpenseTypeDto.name) ?: existingExpenseTypeDto.name,
        expenseAccountCode = expenseAccountCode?.orElse(existingExpenseTypeDto.expenseAccountCode) ?: existingExpenseTypeDto.expenseAccountCode,
        eligiblePayeeTypes = eligiblePayeeTypes?.orElse(existingExpenseTypeDto.eligiblePayeeTypes) ?: existingExpenseTypeDto.eligiblePayeeTypes,
        eligibleSourceTypes = eligibleSourceTypes?.orElse(existingExpenseTypeDto.eligibleSourceTypes) ?: existingExpenseTypeDto.eligibleSourceTypes
    )
}
