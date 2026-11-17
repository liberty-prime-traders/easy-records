package app.myliberty.easy_records.locations.business.expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseRowCommand
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseSubmission
import app.myliberty.easy_records.cross_tier.expense.model.SourceDocument
import app.myliberty.easy_records.cross_tier.expense.operation.ExpenseOperationsFactory
import app.myliberty.easy_records.cross_tier.expense.operation.ExpenseReissuer
import app.myliberty.easy_records.cross_tier.expense.request.ExpensePaymentCreateRequest
import app.myliberty.easy_records.cross_tier.expense.request.ExpensePaymentVoidRequest
import app.myliberty.easy_records.cross_tier.expense.request.ExpenseVoidRequest
import app.myliberty.easy_records.cross_tier.expense.request.PurchaseExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.expense.request.SaleExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.expense.request.StandaloneExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.expense.request.WageExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.organizations.business.expense_type.api.ExpenseTypeService
import app.myliberty.easy_records.organizations.business.expense_type.api.SystemExpenseType
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class ExpenseService(
    expenseOperationsFactory: ExpenseOperationsFactory,
    locationExpenseTier: LocationExpenseTier,
    private val expenseTypeService: ExpenseTypeService,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val saleDataFetcher: SaleDataFetcher
) : ExpenseReissuer {

    private val tierExpenseOperations = expenseOperationsFactory.operationsFor(locationExpenseTier)
    private val expenseOperations = tierExpenseOperations.expenseOperations
    private val expensePaymentOperations = tierExpenseOperations.expensePaymentOperations
    private val expenseReadOperations = tierExpenseOperations.expenseReadOperations
    private val expenseReissueOperations = tierExpenseOperations.expenseReissueOperations

    override val isLocationLevel = true

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseOperations.createStandalone(standaloneExpenseBatchRequest)

    fun createWages(wageExpenseBatchRequest: WageExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val wageExpenseTypeId = expenseTypeService.getBySystemExpenseType(SystemExpenseType.WAGES).id
        return expenseOperations.createBatch(
            ExpenseSourceType.WAGES,
            "${ExpenseSourceType.WAGES.batchDescriptionLabel()} – ${wageExpenseBatchRequest.expenseDate}",
            ExpenseSubmission(
                wageExpenseBatchRequest.expenseDate,
                wageExpenseBatchRequest.rows.map {
                    ExpenseRowCommand(wageExpenseTypeId, it.employeeContactId, it.amount.toBigDecimal(), null, it.expenseDateOverride, it.settlement)
                }
            )
        )
    }

    fun createForPurchase(purchaseExpenseBatchRequest: PurchaseExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoByReferenceNumber(
            StringUtils.getValueOrException(purchaseExpenseBatchRequest.purchaseReference, "Purchase reference is required")
        )
        if (purchaseInfo.purchaseStatus == PurchaseStatus.DRAFT) {
            throw RtsGenericException("Purchase ${purchaseInfo.referenceNumber} is still a draft and cannot carry expenses")
        }
        return expenseOperations.createForSourceDocument(
            SourceDocument(ExpenseSourceType.PURCHASE, purchaseInfo.referenceNumber, purchaseInfo.id),
            ExpenseSubmission(
                purchaseExpenseBatchRequest.expenseDate,
                purchaseExpenseBatchRequest.rows.map {
                    ExpenseRowCommand(
                        it.expenseTypeId, it.payeeContactId ?: purchaseInfo.supplierId, it.amount.toBigDecimal(),
                        it.description, it.expenseDateOverride, it.settlement
                    )
                }
            )
        )
    }

    fun createForSale(saleExpenseBatchRequest: SaleExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val saleHeader = saleDataFetcher.getSaleHeaderByReferenceNumber(
            StringUtils.getValueOrException(saleExpenseBatchRequest.saleReference, "Sale reference is required")
        )
        when (saleHeader.status) {
            SaleStatus.DRAFT -> throw RtsGenericException("Sale ${saleHeader.referenceNumber} is still a draft and cannot carry expenses")
            SaleStatus.DISCARDED -> throw RtsGenericException("Sale ${saleHeader.referenceNumber} was discarded and cannot carry expenses")
            SaleStatus.CONFIRMED, SaleStatus.VOIDED -> Unit
        }
        return expenseOperations.createForSourceDocument(
            SourceDocument(ExpenseSourceType.SALE, saleHeader.referenceNumber, saleHeader.id),
            ExpenseSubmission(saleExpenseBatchRequest.expenseDate, saleExpenseBatchRequest.rows.map { it.toRowCommand() })
        )
    }

    fun recordPayments(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expensePaymentOperations.recordPayments(expensePaymentCreateRequests)

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseOperations.voidExpense(expenseVoidRequest)

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expensePaymentOperations.voidPayment(expensePaymentVoidRequest)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getExpense(expenseReference: String): ExpenseSummaryResponse =
        expenseReadOperations.getExpense(expenseReference)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> =
        expenseReadOperations.getBySource(sourceType, sourceReference)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissueRecorded(expenseId: UUID) = expenseReissueOperations.reissueRecorded(expenseId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissueVoided(expenseVoidId: UUID) = expenseReissueOperations.reissueVoided(expenseVoidId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissuePaymentRecorded(paymentId: UUID) = expenseReissueOperations.reissuePaymentRecorded(paymentId)

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissuePaymentVoided(paymentVoidId: UUID) = expenseReissueOperations.reissuePaymentVoided(paymentVoidId)
}
