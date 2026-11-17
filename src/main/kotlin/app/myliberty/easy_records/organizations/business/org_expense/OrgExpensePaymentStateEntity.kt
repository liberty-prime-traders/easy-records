package app.myliberty.easy_records.organizations.business.org_expense

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.cross_tier.expense.entities.ExpensePaymentStateBase
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT_STATE)
class OrgExpensePaymentStateEntity(expenseId: UUID) : ExpensePaymentStateBase(expenseId)
