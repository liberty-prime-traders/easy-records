package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseRowCommand
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseSubmission
import app.myliberty.easy_records.cross_tier.core.expense.model.PaymentInstruction
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ExpenseRowResolverTest {

    private val expenseTypeService = mock(ExpenseTypeService::class.java)
    private val contactService = mock(ContactService::class.java)
    private val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val expenseRowResolver = ExpenseRowResolver(
        expenseTypeService, contactService, paymentMethodDataFetcher, fiscalPeriodService
    )

    private val batchExpenseDate = LocalDate.of(2026, 3, 10)
    private val supplierContact = contactDto(setOf(ContactType.SUPPLIER))
    private val employeeContact = contactDto(setOf(ContactType.EMPLOYEE))
    private val freightExpenseType = expenseTypeDto(
        eligiblePayeeTypes = setOf(ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER),
        eligibleSourceTypes = setOf(ExpenseSourceType.ADHOC, ExpenseSourceType.PURCHASE)
    )

    @Test
    fun `a payee sharing any contact type with the expense type is accepted`() {
        stub(freightExpenseType, supplierContact)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact))))

        assertEquals(1, resolvedExpenseRows.size)
        assertEquals(supplierContact.id, resolvedExpenseRows.single().payee.id)
    }

    @Test
    fun `a payee with no overlapping contact type is rejected`() {
        stub(freightExpenseType, employeeContact)

        assertThrows(LibertyGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, employeeContact))))
        }
    }

    @Test
    fun `a type that does not list the source type is rejected`() {
        stub(freightExpenseType, supplierContact)

        assertThrows(LibertyGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.SALE, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact))))
        }
    }

    @Test
    fun `a row's own date wins over the batch date and each distinct date is checked once`() {
        stub(freightExpenseType, supplierContact)
        val overrideDate = LocalDate.of(2026, 2, 1)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(
                rowCommand(freightExpenseType, supplierContact, expenseDateOverride = overrideDate),
                rowCommand(freightExpenseType, supplierContact),
                rowCommand(freightExpenseType, supplierContact)
            )))

        assertEquals(overrideDate, resolvedExpenseRows[0].expenseDate)
        assertEquals(batchExpenseDate, resolvedExpenseRows[1].expenseDate)
        verify(fiscalPeriodService, times(1)).requireOpenForDate(batchExpenseDate)
        verify(fiscalPeriodService, times(1)).requireOpenForDate(overrideDate)
    }

    @Test
    fun `a settlement's payment date follows the row's effective expense date unless it declares its own`() {
        stub(freightExpenseType, supplierContact)
        val paymentMethodId = UUID.randomUUID()
        `when`(paymentMethodDataFetcher.findAccountCode(paymentMethodId)).thenReturn("001.001")
        val ownPaymentDate = LocalDate.of(2026, 3, 20)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(
                rowCommand(freightExpenseType, supplierContact, settlement = PaymentInstruction(paymentMethodId)),
                rowCommand(freightExpenseType, supplierContact, settlement = PaymentInstruction(paymentMethodId, paymentDate = ownPaymentDate))
            )))

        assertEquals(batchExpenseDate, resolvedExpenseRows[0].settlement!!.paymentDate)
        assertEquals(ownPaymentDate, resolvedExpenseRows[1].settlement!!.paymentDate)
    }

    @Test
    fun `the transaction maximum itself is accepted`() {
        stub(freightExpenseType, supplierContact)
        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(
            rowCommand(freightExpenseType, supplierContact, amount = Decimals.MAXIMUM_TRANSACTION_AMOUNT)
        )))

        assertEquals(Decimals.MAXIMUM_TRANSACTION_AMOUNT, resolvedExpenseRows.single().amount)
    }

    @Test
    fun `an empty or oversized request is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, emptyList()))
        }
        val tooManyRows = List(ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST + 1) { rowCommand(freightExpenseType, supplierContact) }
        assertThrows(LibertyGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, tooManyRows))
        }
    }

    private fun stub(expenseType: ExpenseTypeDto, contact: ContactDto) {
        `when`(expenseTypeService.getById(expenseType.id)).thenReturn(expenseType)
        `when`(contactService.getContactById(contact.id)).thenReturn(contact)
    }

    private fun rowCommand(
        expenseType: ExpenseTypeDto,
        contact: ContactDto,
        amount: BigDecimal = BigDecimal("50"),
        expenseDateOverride: LocalDate? = null,
        settlement: PaymentInstruction? = null
    ) = ExpenseRowCommand(expenseType.id, contact.id, amount, null, expenseDateOverride, settlement)

    private fun expenseTypeDto(
        eligiblePayeeTypes: Set<ContactType>,
        eligibleSourceTypes: Set<ExpenseSourceType>
    ) = ExpenseTypeDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), code = null,
        name = "Freight", expenseAccountCode = "005.005", eligiblePayeeTypes = eligiblePayeeTypes,
        eligibleSourceTypes = eligibleSourceTypes, systemDefined = false
    )

    private fun contactDto(contactTypes: Set<ContactType>) = ContactDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "CONT01", contactTypes = contactTypes, companyName = "Acme"
    )
}
