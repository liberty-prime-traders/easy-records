package app.myliberty.easy_records.organizations.business.core.org_expense

import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseBase
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE)
@HasReference(tableName = TableName.ORG_EXPENSE)
class OrgExpenseEntity(
    expenseTypeId: UUID,
    expenseAccountCode: String,
    payeeContactId: UUID,
    amount: BigDecimal,
    expenseDate: LocalDate,
    description: String?,
    sourceType: ExpenseSourceType,
    sourceReference: String?,
    batchId: UUID
) : ExpenseBase(expenseTypeId, expenseAccountCode, payeeContactId, amount, expenseDate, description, sourceType, sourceReference, batchId)
