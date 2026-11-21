package app.myliberty.easy_records.organizations.business.core.expense_type.api

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import java.util.UUID

data class ExpenseTypeResponseDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val expenseAccountCode: String,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>,
    val systemDefined: Boolean
)
