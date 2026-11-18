package app.myliberty.easy_records.locations.business.core.expense

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentStateBase
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.EXPENSE_PAYMENT_STATE)
class ExpensePaymentStateEntity(expenseId: UUID) : ExpensePaymentStateBase(expenseId)
