package app.myliberty.easy_records.locations.business.core.expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpense
import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpenseBatch
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDraft
import app.myliberty.easy_records.cross_tier.core.expense.store.ExpensePaymentStateMaintainer
import app.myliberty.easy_records.cross_tier.core.expense.store.JpaExpenseStore
import app.myliberty.easy_records.locations.business.core.expense.ExpenseBatchEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpenseBatchRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentStateEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentStateRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentVoidEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentVoidRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseVoidEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpenseVoidRepository
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema
class LocationExpenseStore(
    private val expenseBatchRepository: ExpenseBatchRepository,
    private val expenseRepository: ExpenseRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val expensePaymentVoidRepository: ExpensePaymentVoidRepository,
    private val expenseVoidRepository: ExpenseVoidRepository,
    private val expensePaymentStateRepository: ExpensePaymentStateRepository
) : JpaExpenseStore(
    expenseBatchRepository, expenseRepository, expensePaymentRepository, expensePaymentVoidRepository, expenseVoidRepository,
    expensePaymentStateRepository
) {

    override fun saveNewBatch(newExpenseBatch: NewExpenseBatch) = expenseBatchRepository.save(
        ExpenseBatchEntity(newExpenseBatch.description, newExpenseBatch.source.type, newExpenseBatch.source.reference)
    )

    override fun saveNewExpenses(newExpenses: List<NewExpense>): List<ExpenseEntity> {
        val expenseEntitys = expenseRepository.saveAll(
            newExpenses.map { newExpense ->
                val resolvedExpenseRow = newExpense.resolvedExpenseRow
                ExpenseEntity(
                    expenseTypeId = resolvedExpenseRow.expenseType.id,
                    expenseAccountCode = resolvedExpenseRow.expenseType.expenseAccountCode,
                    payeeContactId = resolvedExpenseRow.payee.id,
                    amount = resolvedExpenseRow.amount,
                    expenseDate = resolvedExpenseRow.expenseDate,
                    description = resolvedExpenseRow.description,
                    sourceType = newExpense.source.type,
                    sourceReference = newExpense.source.reference,
                    batchId = newExpense.batchId
                )
            }
        )
        expensePaymentStateRepository.saveAll(expenseEntitys.map { ExpensePaymentStateEntity(expenseId = it.id!!) })
        return expenseEntitys
    }

    override fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>, paidTotalsByExpenseId: Map<UUID, BigDecimal>) =
        ExpensePaymentStateMaintainer.refresh(expensePaymentStateRepository, expenseDtos, paidTotalsByExpenseId)

    override fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>) =
        expensePaymentRepository.saveAll(
            expensePaymentDrafts.map { expensePaymentDraft ->
                ExpensePaymentEntity(
                    expenseId = expensePaymentDraft.expenseId,
                    paymentMethodId = expensePaymentDraft.resolvedSettlement.paymentMethodId,
                    paymentMethodAccountCode = expensePaymentDraft.resolvedSettlement.paymentMethodAccountCode,
                    amount = expensePaymentDraft.amount,
                    providerReference = expensePaymentDraft.resolvedSettlement.providerReference,
                    paymentDate = DateTimes.Offset.atStartOfDayInOrganizationZone(expensePaymentDraft.resolvedSettlement.paymentDate)
                )
            }
        )

    override fun saveNewPaymentVoid(paymentId: UUID, reason: String) =
        expensePaymentVoidRepository.save(ExpensePaymentVoidEntity(paymentId, reason))

    override fun saveNewExpenseVoid(expenseId: UUID, reason: String) =
        expenseVoidRepository.save(ExpenseVoidEntity(expenseId, reason))
}
