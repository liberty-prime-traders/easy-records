package app.myliberty.easy_records.organizations.business.org_expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.expense.model.ExpensePaymentDraft
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.expense.model.NewExpense
import app.myliberty.easy_records.cross_tier.expense.model.NewExpenseBatch
import app.myliberty.easy_records.cross_tier.expense.store.ExpensePaymentStateMaintainer
import app.myliberty.easy_records.cross_tier.expense.store.JpaExpenseStore
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseBatchEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseBatchRepository
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentRepository
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentStateEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentStateRepository
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentVoidEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpensePaymentVoidRepository
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseRepository
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseVoidEntity
import app.myliberty.easy_records.organizations.business.org_expense.OrgExpenseVoidRepository
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class OrgExpenseStore(
    private val orgExpenseBatchRepository: OrgExpenseBatchRepository,
    private val orgExpenseRepository: OrgExpenseRepository,
    private val orgExpensePaymentRepository: OrgExpensePaymentRepository,
    private val orgExpensePaymentVoidRepository: OrgExpensePaymentVoidRepository,
    private val orgExpenseVoidRepository: OrgExpenseVoidRepository,
    private val orgExpensePaymentStateRepository: OrgExpensePaymentStateRepository
) : JpaExpenseStore(
    orgExpenseBatchRepository, orgExpenseRepository, orgExpensePaymentRepository, orgExpensePaymentVoidRepository, orgExpenseVoidRepository,
    orgExpensePaymentStateRepository
) {

    override fun saveNewBatch(newExpenseBatch: NewExpenseBatch) = orgExpenseBatchRepository.save(
        OrgExpenseBatchEntity(newExpenseBatch.description, newExpenseBatch.source.type, newExpenseBatch.source.reference)
    )

    override fun saveNewExpenses(newExpenses: List<NewExpense>): List<OrgExpenseEntity> {
        val orgExpenseEntitys = orgExpenseRepository.saveAll(
            newExpenses.map { newExpense ->
                val resolvedExpenseRow = newExpense.resolvedExpenseRow
                OrgExpenseEntity(
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
        orgExpensePaymentStateRepository.saveAll(orgExpenseEntitys.map { OrgExpensePaymentStateEntity(expenseId = it.id!!) })
        return orgExpenseEntitys
    }

    override fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>, paidTotalsByExpenseId: Map<UUID, BigDecimal>) =
        ExpensePaymentStateMaintainer.refresh(orgExpensePaymentStateRepository, expenseDtos, paidTotalsByExpenseId)

    override fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>) =
        orgExpensePaymentRepository.saveAll(
            expensePaymentDrafts.map { expensePaymentDraft ->
                OrgExpensePaymentEntity(
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
        orgExpensePaymentVoidRepository.save(OrgExpensePaymentVoidEntity(paymentId, reason))

    override fun saveNewExpenseVoid(expenseId: UUID, reason: String) =
        orgExpenseVoidRepository.save(OrgExpenseVoidEntity(expenseId, reason))
}
