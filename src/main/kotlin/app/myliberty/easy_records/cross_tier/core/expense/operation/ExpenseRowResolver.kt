package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseSubmission
import app.myliberty.easy_records.cross_tier.core.expense.model.PaymentInstruction
import app.myliberty.easy_records.cross_tier.core.expense.model.ResolvedExpenseRow
import app.myliberty.easy_records.cross_tier.core.expense.model.ResolvedSettlement
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ExpenseRowResolver(
    private val expenseTypeService: ExpenseTypeService,
    private val contactService: ContactService,
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun guardRowCount(rowCount: Int) {
        if (rowCount == 0) throw LibertyGenericException("At least one expense row is required")
        if (rowCount > MAXIMUM_ROWS_PER_REQUEST) {
            throw LibertyGenericException("A request may contain at most $MAXIMUM_ROWS_PER_REQUEST expense rows")
        }
    }

    fun resolve(sourceType: ExpenseSourceType, expenseSubmission: ExpenseSubmission): List<ResolvedExpenseRow> {
        guardRowCount(expenseSubmission.rowCommands.size)
        val expenseTypesById = HashMap<UUID, ExpenseTypeDto>()
        val payeesById = HashMap<UUID, ContactDto>()
        val openDates = HashSet<LocalDate>()
        return expenseSubmission.rowCommands.map { rowCommand ->
            val expenseType = expenseTypesById.getOrPut(rowCommand.expenseTypeId) {
                expenseTypeService.getById(rowCommand.expenseTypeId)
            }
            if (sourceType !in expenseType.eligibleSourceTypes) {
                throw LibertyGenericException("Expense type ${expenseType.name} cannot be used from this screen")
            }
            val payee = payeesById.getOrPut(rowCommand.payeeContactId) {
                contactService.getContactById(rowCommand.payeeContactId)
            }
            if (payee.contactTypes.intersect(expenseType.eligiblePayeeTypes).isEmpty()) {
                throw LibertyGenericException(
                    "The selected payee (${payee.identity.displayName}) is not eligible for expense type: ${expenseType.name}"
                )
            }
            val expenseDate = rowCommand.expenseDateOverride ?: expenseSubmission.expenseDate
            requireOpenPeriod(openDates, expenseDate)
            ResolvedExpenseRow(
                expenseType = expenseType,
                payee = payee,
                amount = rowCommand.amount,
                description = StringUtils.getValueOrNull(rowCommand.description),
                expenseDate = expenseDate,
                settlement = rowCommand.settlement?.let { resolveSettlement(it, expenseDate, openDates) }
            )
        }
    }

    fun resolveSettlement(
        paymentInstruction: PaymentInstruction,
        defaultPaymentDate: LocalDate,
        openDates: MutableSet<LocalDate> = HashSet()
    ): ResolvedSettlement {
        val paymentMethodAccountCode = paymentMethodDataFetcher.findAccountCode(paymentInstruction.paymentMethodId)
        val paymentDate = paymentInstruction.paymentDate ?: defaultPaymentDate
        requireOpenPeriod(openDates, paymentDate)
        return ResolvedSettlement(
            paymentMethodId = paymentInstruction.paymentMethodId,
            paymentMethodAccountCode = paymentMethodAccountCode,
            providerReference = StringUtils.getValueOrNull(paymentInstruction.paymentReference),
            paymentDate = paymentDate
        )
    }

    fun requireOpenPeriodToday() = fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())

    private fun requireOpenPeriod(alreadyChecked: MutableSet<LocalDate>, date: LocalDate) {
        if (alreadyChecked.add(date)) fiscalPeriodService.requireOpenForDate(date)
    }

    companion object {
        const val MAXIMUM_ROWS_PER_REQUEST = 300
    }
}
