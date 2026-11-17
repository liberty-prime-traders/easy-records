package app.myliberty.easy_records.locations.business.expense

import app.myliberty.easy_records.cross_tier.expense.entities.ExpenseBase
import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.EXPENSE)
@HasReference(tableName = TableName.EXPENSE)
class ExpenseEntity(
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
