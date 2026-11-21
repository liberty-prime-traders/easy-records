package app.myliberty.easy_records.organizations.business.core.expense_type.api

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType

data class ExpenseTypeInsertDto(
    val name: String,
    val expenseAccountCode: String,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>
)
