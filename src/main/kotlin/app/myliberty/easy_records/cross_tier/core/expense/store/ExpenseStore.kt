package app.myliberty.easy_records.cross_tier.core.expense.store

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseAggregate
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseBatchDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDraft
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentVoidDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseVoidDto
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpense
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpenseBatch
import java.math.BigDecimal
import java.util.UUID

interface ExpenseStore {

    fun createBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchDto

    fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchDto?

    fun saveExpenses(newExpenses: List<NewExpense>): List<ExpenseDto>

    fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentDto>

    fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>, paidTotalsByExpenseId: Map<UUID, BigDecimal>)

    fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidDto

    fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidDto

    fun findExpenseById(expenseId: UUID): ExpenseDto?

    fun findExpenseByReference(referenceNumber: String): ExpenseDto?

    fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseDto>

    fun findPaymentById(paymentId: UUID): ExpensePaymentDto?

    fun findPaymentByReference(referenceNumber: String): ExpensePaymentDto?

    fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidDto?

    fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidDto?

    fun loadForBatch(batchId: UUID): ExpenseAggregate

    fun loadForExpenses(expenseIds: Collection<UUID>): ExpenseAggregate
}
