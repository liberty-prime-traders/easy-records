package app.myliberty.easy_records.locations.business.core.expense.api

import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseSource

import app.myliberty.easy_records.cross_tier.core.expense.model.NewExpense

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.model.ResolvedExpenseRow
import app.myliberty.easy_records.locations.business.core.expense.ExpenseBatchRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentStateEntity
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentStateRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpensePaymentVoidRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseRepository
import app.myliberty.easy_records.locations.business.core.expense.ExpenseVoidRepository
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LocationExpenseStoreTest {

    private val expenseRepository = mock(ExpenseRepository::class.java)
    private val expensePaymentStateRepository = mock(ExpensePaymentStateRepository::class.java)

    private val locationExpenseStore = LocationExpenseStore(
        mock(ExpenseBatchRepository::class.java), expenseRepository, mock(ExpensePaymentRepository::class.java),
        mock(ExpensePaymentVoidRepository::class.java), mock(ExpenseVoidRepository::class.java),
        expensePaymentStateRepository
    )

    @Test
    fun `saving an expense creates its payment state as unpaid with nothing paid`() {
        val expenseEntity = persistedExpenseEntity()
        `when`(expenseRepository.saveAll(anyIterable<ExpenseEntity>())).thenReturn(listOf(expenseEntity))

        locationExpenseStore.saveExpenses(listOf(NewExpense(expenseEntity.batchId, ExpenseSource(ExpenseSourceType.ADHOC, null), resolvedExpenseRow())))

        @Suppress("UNCHECKED_CAST")
        val savedStates = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<ExpensePaymentStateEntity>>
        verify(expensePaymentStateRepository).saveAll(savedStates.capture())
        val savedState = savedStates.value.single()
        assertEquals(expenseEntity.id, savedState.expenseId)
        assertEquals(PaymentStatus.UNPAID, savedState.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(savedState.amountPaid))
    }

    @Test
    fun `refreshing derives each expense's status and paid amount from its active payments`() {
        val partiallyPaidExpense = expenseDto("100")
        val fullyPaidExpense = expenseDto("50")
        val partiallyPaidState = ExpensePaymentStateEntity(partiallyPaidExpense.id)
        val fullyPaidState = ExpensePaymentStateEntity(fullyPaidExpense.id)
        stubStates(partiallyPaidState, fullyPaidState)

        locationExpenseStore.refreshPaymentStates(
            listOf(partiallyPaidExpense, fullyPaidExpense),
            mapOf(partiallyPaidExpense.id to BigDecimal("40"), fullyPaidExpense.id to BigDecimal("50"))
        )

        assertEquals(PaymentStatus.PARTIALLY_SETTLED, partiallyPaidState.paymentStatus)
        assertEquals(0, BigDecimal("40").compareTo(partiallyPaidState.amountPaid))
        assertEquals(PaymentStatus.FULLY_SETTLED, fullyPaidState.paymentStatus)
        assertEquals(0, BigDecimal("50").compareTo(fullyPaidState.amountPaid))
    }

    @Test
    fun `an expense whose payments are all voided drops back to unpaid`() {
        val expenseDto = expenseDto("100")
        val paidState = ExpensePaymentStateEntity(expenseDto.id).apply {
            paymentStatus = PaymentStatus.FULLY_SETTLED
            amountPaid = BigDecimal("100")
        }
        stubStates(paidState)

        locationExpenseStore.refreshPaymentStates(listOf(expenseDto), mapOf(expenseDto.id to BigDecimal.ZERO))

        assertEquals(PaymentStatus.UNPAID, paidState.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(paidState.amountPaid))
    }

    @Test
    fun `refreshing nothing does not touch the database`() {
        locationExpenseStore.refreshPaymentStates(emptyList(), emptyMap())

        verify(expensePaymentStateRepository, never()).findByExpenseIdIn(anyCollection())
    }

    private fun stubStates(vararg states: ExpensePaymentStateEntity) {
        `when`(expensePaymentStateRepository.findByExpenseIdIn(anyCollection())).thenReturn(states.toList())
    }

    private fun expenseDto(amount: String) = ExpenseDto(
        UUID.randomUUID(), "EXPN01", UUID.randomUUID(), "005.005", UUID.randomUUID(), BigDecimal(amount),
        LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun persistedExpenseEntity() = ExpenseEntity(
        expenseTypeId = UUID.randomUUID(),
        expenseAccountCode = "005.005",
        payeeContactId = UUID.randomUUID(),
        amount = BigDecimal("100.0000"),
        expenseDate = LocalDate.of(2026, 3, 10),
        description = null,
        sourceType = ExpenseSourceType.ADHOC,
        sourceReference = null,
        batchId = UUID.randomUUID()
    ).apply {
        id = UUID.randomUUID()
        createdOn = OffsetDateTime.now()
        createdById = UUID.randomUUID()
        referenceNumber = "EXPN000001"
    }

    private fun resolvedExpenseRow(): ResolvedExpenseRow {
        val expenseType = mock(ExpenseTypeDto::class.java)
        `when`(expenseType.id).thenReturn(UUID.randomUUID())
        `when`(expenseType.expenseAccountCode).thenReturn("005.005")
        val payee = mock(ContactDto::class.java)
        `when`(payee.id).thenReturn(UUID.randomUUID())
        return ResolvedExpenseRow(expenseType, payee, BigDecimal("100.0000"), null, LocalDate.of(2026, 3, 10), null)
    }
}
