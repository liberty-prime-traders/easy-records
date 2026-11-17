package app.myliberty.easy_records.organizations.business.org_expense

import app.myliberty.easy_records.cross_tier.expense.entities.ExpensePaymentBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT)
@HasReference(tableName = TableName.ORG_EXPENSE_PAYMENT)
class OrgExpensePaymentEntity(
    expenseId: UUID,
    paymentMethodId: UUID,
    paymentMethodAccountCode: String,
    amount: BigDecimal,
    providerReference: String?,
    paymentDate: OffsetDateTime
) : ExpensePaymentBase(expenseId, paymentMethodId, paymentMethodAccountCode, amount, providerReference, paymentDate)
