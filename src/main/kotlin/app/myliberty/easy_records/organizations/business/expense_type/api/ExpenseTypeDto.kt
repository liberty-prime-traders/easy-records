package app.myliberty.easy_records.organizations.business.expense_type.api

import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.contact.api.ContactType
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val code: String?,
    val name: String,
    val expenseAccountCode: String,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>,
    val systemDefined: Boolean
)
