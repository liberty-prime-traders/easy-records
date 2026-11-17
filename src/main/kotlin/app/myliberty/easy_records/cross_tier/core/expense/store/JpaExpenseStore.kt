package app.myliberty.easy_records.cross_tier.core.expense.store

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseBase
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseBatchBase
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentBase
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentStateBase
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentVoidBase
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpenseVoidBase
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseAggregate
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseBatchDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDraft
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentVoidDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseVoidDto
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpense
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpenseBatch
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseBatchRepositoryBase
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpensePaymentRepositoryBase
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpensePaymentStateRepositoryBase
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpensePaymentVoidRepositoryBase
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseRepositoryBase
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseVoidRepositoryBase
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.util.UUID

abstract class JpaExpenseStore(
    private val expenseBatchRepository: ExpenseBatchRepositoryBase<out ExpenseBatchBase>,
    private val expenseRepository: ExpenseRepositoryBase<out ExpenseBase>,
    private val expensePaymentRepository: ExpensePaymentRepositoryBase<out ExpensePaymentBase>,
    private val expensePaymentVoidRepository: ExpensePaymentVoidRepositoryBase<out ExpensePaymentVoidBase>,
    private val expenseVoidRepository: ExpenseVoidRepositoryBase<out ExpenseVoidBase>,
    private val expensePaymentStateRepository: ExpensePaymentStateRepositoryBase<out ExpensePaymentStateBase>
) : ExpenseStore {

    protected abstract fun saveNewBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchBase

    protected abstract fun saveNewExpenses(newExpenses: List<NewExpense>): List<ExpenseBase>

    protected abstract fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentBase>

    protected abstract fun saveNewPaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidBase

    protected abstract fun saveNewExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidBase

    override fun createBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchDto =
        ExpenseDtoMapper.toDto(saveNewBatch(newExpenseBatch))

    override fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchDto? =
        expenseBatchRepository.findBySourceTypeAndSourceReference(sourceType, sourceReference)?.let { ExpenseDtoMapper.toDto(it) }

    override fun saveExpenses(newExpenses: List<NewExpense>): List<ExpenseDto> =
        saveNewExpenses(newExpenses).map { ExpenseDtoMapper.toDto(it) }

    override fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentDto> =
        saveNewPayments(expensePaymentDrafts).map { ExpenseDtoMapper.toDto(it) }

    override fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidDto =
        ExpenseDtoMapper.toDto(saveNewExpenseVoid(expenseId, reason))

    override fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidDto =
        ExpenseDtoMapper.toDto(saveNewPaymentVoid(paymentId, reason))

    override fun findExpenseById(expenseId: UUID): ExpenseDto? =
        expenseRepository.findById(expenseId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findExpenseByReference(referenceNumber: String): ExpenseDto? =
        expenseRepository.findByReferenceNumber(referenceNumber)?.let { ExpenseDtoMapper.toDto(it) }

    override fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseDto> =
        expenseRepository.findByReferenceNumberIn(referenceNumbers).map { ExpenseDtoMapper.toDto(it) }

    override fun findPaymentById(paymentId: UUID): ExpensePaymentDto? =
        expensePaymentRepository.findById(paymentId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findPaymentByReference(referenceNumber: String): ExpensePaymentDto? =
        expensePaymentRepository.findByReferenceNumber(referenceNumber)?.let { ExpenseDtoMapper.toDto(it) }

    override fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidDto? =
        expenseVoidRepository.findById(expenseVoidId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidDto? =
        expensePaymentVoidRepository.findById(paymentVoidId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun loadForBatch(batchId: UUID): ExpenseAggregate {
        val batchEntity = expenseBatchRepository.findById(batchId)
            .orElseThrow { RtsGenericException("Expense batch $batchId not found") }
        return assemble(listOf(batchEntity), expenseRepository.findByBatchId(batchId))
    }

    override fun loadForExpenses(expenseIds: Collection<UUID>): ExpenseAggregate {
        val expenseEntities = expenseRepository.findAllById(expenseIds)
        val missingExpenseIds = expenseIds.toSet() - expenseEntities.map { it.id!! }.toSet()
        if (missingExpenseIds.isNotEmpty()) throw RtsGenericException("Expense $missingExpenseIds not found")
        return assemble(expenseBatchRepository.findAllById(expenseEntities.map { it.batchId }.toSet()), expenseEntities)
    }

    private fun assemble(batchEntities: List<ExpenseBatchBase>, expenseEntities: List<ExpenseBase>): ExpenseAggregate {
        val expenseIds = expenseEntities.map { it.id!! }
        val paymentEntities = if (expenseIds.isEmpty()) emptyList() else expensePaymentRepository.findByExpenseIdIn(expenseIds)
        val paymentVoidEntities = if (paymentEntities.isEmpty()) emptyList() else
            expensePaymentVoidRepository.findByExpensePaymentIdIn(paymentEntities.map { it.id!! })
        val expenseVoidEntities = if (expenseIds.isEmpty()) emptyList() else expenseVoidRepository.findByExpenseIdIn(expenseIds)
        val paymentStateEntities = if (expenseIds.isEmpty()) emptyList() else expensePaymentStateRepository.findByExpenseIdIn(expenseIds)
        return ExpenseAggregate(
            batches = batchEntities.map { ExpenseDtoMapper.toDto(it) },
            expenses = expenseEntities.map { ExpenseDtoMapper.toDto(it) },
            payments = paymentEntities.map { ExpenseDtoMapper.toDto(it) },
            paymentVoids = paymentVoidEntities.map { ExpenseDtoMapper.toDto(it) },
            expenseVoids = expenseVoidEntities.map { ExpenseDtoMapper.toDto(it) },
            paymentStates = paymentStateEntities.map { ExpenseDtoMapper.toDto(it) }
        )
    }
}
