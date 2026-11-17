package app.myliberty.easy_records.organizations.business.expense_type.api

import java.util.UUID

data class ExpenseTypeRenameDto(
    val id: UUID,
    val name: String
)
