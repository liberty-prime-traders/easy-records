package app.myliberty.easy_records.locations.business.core.expense

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseVoidBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.EXPENSE_VOID)
@HasReference(tableName = TableName.EXPENSE_VOID)
class ExpenseVoidEntity(
    expenseId: UUID,
    reason: String
) : ExpenseVoidBase(expenseId, reason)
