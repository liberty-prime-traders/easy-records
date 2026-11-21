package app.myliberty.easy_records.organizations.business.core.org_expense

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseVoidBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_VOID)
@HasReference(tableName = TableName.ORG_EXPENSE_VOID)
class OrgExpenseVoidEntity(
    expenseId: UUID,
    reason: String
) : ExpenseVoidBase(expenseId, reason)
