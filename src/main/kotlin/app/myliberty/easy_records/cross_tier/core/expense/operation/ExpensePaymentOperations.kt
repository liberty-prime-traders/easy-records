package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseTier
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseAggregate
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDraft
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ResolvedSettlement
import app.myliberty.easy_records.cross_tier.core.expense.model.SettledExpense
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentCreateRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.business.PaymentStatusResolver
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ExpensePaymentOperations(
    private val expenseTier: ExpenseTier,
    private val expenseEvents: ExpenseEvents,
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup
) {

    private val expenseStore = expenseTier.expenseStore

    fun recordPayments(
        expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>
    ): List<ExpenseSummaryResponse> {
        requireWithinRequestLimits(expensePaymentCreateRequests)
        val resolvedPaymentRequestsByExpenseReference = resolveByExpenseReference(expensePaymentCreateRequests)
        val expenseDtosByReference = expenseLookup.requireExpenses(resolvedPaymentRequestsByExpenseReference.keys.toList())
        val expenseAggregatesById = lockAndLoad(expenseDtosByReference.values)
        val expensePaymentDrafts = resolvedPaymentRequestsByExpenseReference.flatMap { (expenseReference, resolvedPaymentRequests) ->
            val expenseDto = expenseDtosByReference.getValue(expenseReference)
            planPayments(expenseDto, expenseAggregatesById.getValue(expenseDto.id), resolvedPaymentRequests)
        }
        saveAndPublish(expensePaymentDrafts, expenseDtosByReference.values)
        val paidTotalsByExpenseId = expenseDtosByReference.values.associate { expenseDto ->
            val recordedAmounts = expensePaymentDrafts.filter { it.expenseId == expenseDto.id }.map { it.amount }
            expenseDto.id to PaymentStatusResolver.paidTotalAfterRecording(
                expenseAggregatesById.getValue(expenseDto.id).activePaidAmount(expenseDto.id), recordedAmounts
            )
        }
        expenseStore.refreshPaymentStates(expenseDtosByReference.values, paidTotalsByExpenseId)
        val summariesByReference = expenseResponseBuilder
            .buildSummaries(expenseStore.loadForExpenses(expenseDtosByReference.values.map { it.id }))
            .associateBy { it.reference }
        return resolvedPaymentRequestsByExpenseReference.keys.map { summariesByReference.getValue(it) }
    }

    private fun requireWithinRequestLimits(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>) {
        if (expensePaymentCreateRequests.isEmpty()) throw LibertyGenericException("At least one payment is required")
        if (expensePaymentCreateRequests.size > ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST) {
            throw LibertyGenericException("A request can carry at most ${ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST} payments")
        }
    }

    private fun resolveByExpenseReference(
        expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>
    ): Map<String, List<ResolvedPaymentRequest>> {
        val defaultPaymentDate = DateTimes.Local.Now.organization()
        val openDates = HashSet<LocalDate>()
        return expensePaymentCreateRequests
            .map {
                val resolvedSettlement = expenseRowResolver.resolveSettlement(it.settlement, defaultPaymentDate, openDates)
                ResolvedPaymentRequest(it, resolvedSettlement)
            }
            .groupBy { it.expensePaymentCreateRequest.expenseReference }
    }

    private fun lockAndLoad(expenseDtos: Collection<ExpenseDto>): Map<UUID, ExpenseAggregate> {
        val expenseIds = expenseDtos.map { it.id }
        expenseTier.lockExpenses(expenseIds)
        val loadedAggregate = expenseStore.loadForExpenses(expenseIds)
        return expenseDtos.associate { it.id to loadedAggregate.forExpense(it.id) }
    }

    private fun planPayments(
        expenseDto: ExpenseDto,
        expenseAggregate: ExpenseAggregate,
        resolvedPaymentRequests: List<ResolvedPaymentRequest>
    ): List<ExpensePaymentDraft> {
        expenseLookup.requireNotVoided(expenseAggregate, expenseDto)
        val expensePaymentDrafts = resolvedPaymentRequests.map { (paymentRequest, resolvedSettlement) ->
            ExpensePaymentDraft(expenseDto.id, paymentRequest.amount.toBigDecimal(), resolvedSettlement)
        }
        val remainingBalance = expenseDto.amount - expenseAggregate.activePaidAmount(expenseDto.id)
        val totalRequested = expensePaymentDrafts.sumOf { it.amount }
        if (totalRequested > remainingBalance) {
            val formattedRemainingBalance = DisplayFormatters.formatCurrency(remainingBalance)
            val formattedTotalRequested = DisplayFormatters.formatCurrency(totalRequested)
            val formattedDifference = DisplayFormatters.formatCurrency(totalRequested - remainingBalance)
            throw LibertyGenericException(
                "Payments totalling $formattedTotalRequested exceed the remaining balance of $formattedRemainingBalance " +
                    "on ${expenseDto.referenceNumber} by $formattedDifference"
            )
        }
        return expensePaymentDrafts
    }

    private fun saveAndPublish(expensePaymentDrafts: List<ExpensePaymentDraft>, expenseDtos: Collection<ExpenseDto>) {
        publishRecorded(expenseStore.savePayments(expensePaymentDrafts), expenseDtos)
    }

    fun settleNewExpenses(settledExpenses: List<SettledExpense>) {
        if (settledExpenses.isEmpty()) return
        val expensePaymentDrafts = settledExpenses.map {
            ExpensePaymentDraft(it.expenseDto.id, it.expenseDto.amount, it.resolvedSettlement)
        }
        val expenseDtos = settledExpenses.map { it.expenseDto }
        publishRecorded(expenseStore.savePayments(expensePaymentDrafts), expenseDtos)
        val paidTotalsByExpenseId = settledExpenses.associate {
            it.expenseDto.id to PaymentStatusResolver.paidTotalAfterRecording(BigDecimal.ZERO, listOf(it.expenseDto.amount))
        }
        expenseStore.refreshPaymentStates(expenseDtos, paidTotalsByExpenseId)
    }

    private fun publishRecorded(savedPaymentDtos: List<ExpensePaymentDto>, expenseDtos: Collection<ExpenseDto>) {
        val expenseDtosById = expenseDtos.associateBy { it.id }
        savedPaymentDtos.forEach { expensePaymentDto ->
            expenseEvents.publishPaymentRecorded(expensePaymentDto, expenseDtosById.getValue(expensePaymentDto.expenseId))
        }
    }

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse {
        val voidReason = StringUtils.getValueOrException(expensePaymentVoidRequest.reason, "A void reason is required")
        expenseRowResolver.requireOpenPeriodToday()
        val expensePaymentDto = expenseStore.findPaymentByReference(expensePaymentVoidRequest.paymentReference)
            ?: throw LibertyGenericException("Expense payment ${expensePaymentVoidRequest.paymentReference} not found")
        expenseTier.lockExpense(expensePaymentDto.expenseId)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expensePaymentDto.expenseId))
        if (expenseAggregate.paymentVoids.any { it.paymentId == expensePaymentDto.id }) {
            throw LibertyGenericException("Payment ${expensePaymentDto.referenceNumber} has already been voided")
        }
        val expenseDto = expenseAggregate.expenses.single()
        val expensePaymentVoidDto = expenseStore.savePaymentVoid(expensePaymentDto.id, voidReason)
        val paidTotalAfterVoid = PaymentStatusResolver.paidTotalAfterVoid(
            expenseAggregate.activePaidAmount(expenseDto.id), expensePaymentDto.amount
        )
        expenseStore.refreshPaymentStates(listOf(expenseDto), mapOf(expenseDto.id to paidTotalAfterVoid))
        expenseEvents.publishPaymentVoided(expensePaymentVoidDto, expensePaymentDto, expenseDto)
        return expenseResponseBuilder.buildSummary(expenseStore.loadForExpenses(listOf(expenseDto.id)), expenseDto.id)
    }
}

private data class ResolvedPaymentRequest(
    val expensePaymentCreateRequest: ExpensePaymentCreateRequest,
    val resolvedSettlement: ResolvedSettlement
)
