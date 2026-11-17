package app.myliberty.easy_records.organizations.business.expense_type.api

import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.contact.api.ContactType

data class ExpenseTypeInsertDto(
    val name: String,
    val expenseAccountCode: String,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>
)
