package app.myliberty.easy_records.organizations.business.core.org_expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentCreateRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpenseVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.StandaloneExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.StockTransferExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.operation.ExpenseOperationsFactory
import app.myliberty.easy_records.cross_tier.core.expense.operation.ExpenseReissuer
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseSubmission
import app.myliberty.easy_records.cross_tier.core.expense.model.SourceDocument
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrgExpenseService(
    expenseOperationsFactory: ExpenseOperationsFactory,
    orgExpenseTier: OrgExpenseTier,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher
) : ExpenseReissuer {

    private val tierExpenseOperations = expenseOperationsFactory.operationsFor(orgExpenseTier)
    private val expenseOperations = tierExpenseOperations.expenseOperations
    private val expensePaymentOperations = tierExpenseOperations.expensePaymentOperations
    private val expenseReadOperations = tierExpenseOperations.expenseReadOperations
    private val expenseReissueOperations = tierExpenseOperations.expenseReissueOperations

    override val isLocationLevel = false

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseOperations.createStandalone(standaloneExpenseBatchRequest)

    fun createForStockTransfer(stockTransferExpenseBatchRequest: StockTransferExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val stockTransferOrder = stockTransferOrderDataFetcher.getByReferenceNumber(
            StringUtils.getValueOrException(stockTransferExpenseBatchRequest.transferReference, "Transfer reference is required")
        )
        if (stockTransferOrder.status == StockTransferStatus.DRAFT) {
            throw RtsGenericException("Transfer ${stockTransferOrder.referenceNumber} is still a draft and cannot carry expenses")
        }
        return expenseOperations.createForSourceDocument(
            SourceDocument(ExpenseSourceType.STOCK_TRANSFER, stockTransferOrder.referenceNumber, stockTransferOrder.id),
            ExpenseSubmission(stockTransferExpenseBatchRequest.expenseDate, stockTransferExpenseBatchRequest.rows.map { it.toRowCommand() })
        )
    }

    fun recordPayments(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expensePaymentOperations.recordPayments(expensePaymentCreateRequests)

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseOperations.voidExpense(expenseVoidRequest)

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expensePaymentOperations.voidPayment(expensePaymentVoidRequest)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getExpense(expenseReference: String): ExpenseSummaryResponse =
        expenseReadOperations.getExpense(expenseReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> =
        expenseReadOperations.getBySource(sourceType, sourceReference)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueRecorded(expenseId: UUID) = expenseReissueOperations.reissueRecorded(expenseId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissueVoided(expenseVoidId: UUID) = expenseReissueOperations.reissueVoided(expenseVoidId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentRecorded(paymentId: UUID) = expenseReissueOperations.reissuePaymentRecorded(paymentId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissuePaymentVoided(paymentVoidId: UUID) = expenseReissueOperations.reissuePaymentVoided(paymentVoidId)
}
