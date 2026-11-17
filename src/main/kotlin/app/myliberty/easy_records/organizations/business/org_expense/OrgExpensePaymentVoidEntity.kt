package app.myliberty.easy_records.organizations.business.org_expense

import app.myliberty.easy_records.cross_tier.expense.entities.ExpensePaymentVoidBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT_VOID)
@HasReference(tableName = TableName.ORG_EXPENSE_PAYMENT_VOID)
class OrgExpensePaymentVoidEntity(
    expensePaymentId: UUID,
    reason: String
) : ExpensePaymentVoidBase(expensePaymentId, reason)
