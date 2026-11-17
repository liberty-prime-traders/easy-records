package app.myliberty.easy_records.locations.business.expense.api

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseAggregate
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseBatchDto
import app.myliberty.easy_records.cross_tier.expense.model.ExpensePaymentDto
import app.myliberty.easy_records.cross_tier.expense.model.ExpensePaymentStateDto
import app.myliberty.easy_records.cross_tier.expense.model.ExpensePaymentVoidDto
import app.myliberty.easy_records.cross_tier.expense.model.ExpenseDto
import app.myliberty.easy_records.locations.business.expense.ExpenseBatchRepository
import app.myliberty.easy_records.locations.business.expense.ExpensePaymentRepository
import app.myliberty.easy_records.locations.business.expense.ExpensePaymentStateEntity
import app.myliberty.easy_records.locations.business.expense.ExpensePaymentStateRepository
import app.myliberty.easy_records.locations.business.expense.ExpensePaymentVoidRepository
import app.myliberty.easy_records.locations.business.expense.ExpenseRepository
import app.myliberty.easy_records.locations.business.expense.ExpenseVoidRepository
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.expense_type.api.ExpenseTypeResponseDto
import app.myliberty.easy_records.organizations.business.expense_type.api.ExpenseTypeService
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.enums.PaymentStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// The stored state backs both the search filters and every read. This pins the refresh against an
// independent derivation from payments and voids, and checks the builder reports the stored row as is.
class ExpensePaymentStateConsistencyTest {

    private val expenseTypeId = UUID.randomUUID()
    private val paymentMethodId = UUID.randomUUID()
    private val creatorId = UUID.randomUUID()
    private val batchDto = ExpenseBatchDto(UUID.randomUUID(), "EXBT01", "Batch", OffsetDateTime.now(), creatorId)

    private val expensePaymentStateRepository = mock(ExpensePaymentStateRepository::class.java)
    private val locationExpenseStore = LocationExpenseStore(
        mock(ExpenseBatchRepository::class.java), mock(ExpenseRepository::class.java), mock(ExpensePaymentRepository::class.java),
        mock(ExpensePaymentVoidRepository::class.java), mock(ExpenseVoidRepository::class.java),
        expensePaymentStateRepository
    )
    private val expenseResponseBuilder = responseBuilder()

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `the refreshed state matches the active payments for every payment and void combination and the builder reports it unchanged`() {
        val scenarios = listOf(
            emptyList(),
            listOf(PaymentScenario("40", voided = false)),
            listOf(PaymentScenario("100", voided = false)),
            listOf(PaymentScenario("60", voided = true)),
            listOf(PaymentScenario("100", voided = true)),
            listOf(PaymentScenario("40", voided = false), PaymentScenario("60", voided = true)),
            listOf(PaymentScenario("30", voided = false), PaymentScenario("30", voided = false), PaymentScenario("40", voided = true)),
            listOf(PaymentScenario("100", voided = true), PaymentScenario("100", voided = false))
        )

        scenarios.forEach { paymentScenarios ->
            val expenseDto = expenseDto("100")
            val expenseAggregate = aggregate(expenseDto, paymentScenarios)
            val storedState = refreshedState(expenseDto, expenseAggregate)

            val expectedPaid = paymentScenarios.filterNot { it.voided }.sumOf { BigDecimal(it.amount) }
            val expectedStatus = when {
                expectedPaid.signum() == 0 -> PaymentStatus.UNPAID
                expectedPaid >= expenseDto.amount -> PaymentStatus.FULLY_SETTLED
                else -> PaymentStatus.PARTIALLY_SETTLED
            }
            assertEquals(expectedStatus, storedState.paymentStatus, "status for $paymentScenarios")
            assertEquals(0, expectedPaid.compareTo(storedState.amountPaid), "amount paid for $paymentScenarios")

            val storedStateRecord = ExpensePaymentStateDto(expenseDto.id, storedState.paymentStatus, storedState.amountPaid)
            val builtSummary = expenseResponseBuilder.buildSummary(expenseAggregate.copy(paymentStates = listOf(storedStateRecord)), expenseDto.id)
            assertEquals(storedState.paymentStatus, builtSummary.status, "built status for $paymentScenarios")
            assertEquals(0, storedState.amountPaid.compareTo(builtSummary.amountPaid), "built amount paid for $paymentScenarios")
            assertEquals(0, (expenseDto.amount - storedState.amountPaid).compareTo(builtSummary.balanceRemaining), "balance for $paymentScenarios")
        }
    }

    private data class PaymentScenario(val amount: String, val voided: Boolean)

    // Independent derivation of the paid total: a payment counts unless a void row points at it.
    private fun refreshedState(expenseDto: ExpenseDto, expenseAggregate: ExpenseAggregate): ExpensePaymentStateEntity {
        val voidedPaymentIds = expenseAggregate.paymentVoids.map { it.paymentId }.toSet()
        val activePaid = expenseAggregate.payments.filter { it.id !in voidedPaymentIds }.sumOf { it.amount }
        val storedState = ExpensePaymentStateEntity(expenseDto.id)
        `when`(expensePaymentStateRepository.findByExpenseIdIn(anyCollection())).thenReturn(listOf(storedState))

        locationExpenseStore.refreshPaymentStates(listOf(expenseDto), mapOf(expenseDto.id to activePaid))
        return storedState
    }

    private fun aggregate(expenseDto: ExpenseDto, paymentScenarios: List<PaymentScenario>): ExpenseAggregate {
        val payments = paymentScenarios.map {
            ExpensePaymentDto(
                UUID.randomUUID(), expenseDto.id, "EXPY01", paymentMethodId, "001.001", BigDecimal(it.amount),
                null, OffsetDateTime.now(), OffsetDateTime.now()
            )
        }
        val paymentVoids = payments.zip(paymentScenarios).filter { (_, scenario) -> scenario.voided }.map { (payment, _) ->
            ExpensePaymentVoidDto(UUID.randomUUID(), payment.id, "wrong", OffsetDateTime.now())
        }
        return ExpenseAggregate(listOf(batchDto), listOf(expenseDto), payments, paymentVoids, emptyList())
    }

    private fun expenseDto(amount: String) = ExpenseDto(
        UUID.randomUUID(), "EXPN01", expenseTypeId, "005.005", UUID.randomUUID(), BigDecimal(amount),
        LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, batchDto.id, OffsetDateTime.now(), creatorId
    )

    private fun responseBuilder(): ExpenseResponseBuilder {
        val expenseTypeService = mock(ExpenseTypeService::class.java)
        `when`(expenseTypeService.getAll(null)).thenReturn(
            listOf(ExpenseTypeResponseDto(expenseTypeId, null, "Freight", "005.005", emptySet(), emptySet(), false))
        )
        val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java)
        `when`(paymentMethodDataFetcher.getNamesById()).thenReturn(mapOf(paymentMethodId to "Cash"))
        val userQualifier = mock(UserQualifier::class.java)
        `when`(userQualifier.getCreatorFullName(creatorId)).thenReturn("Creator")
        return ExpenseResponseBuilder(
            expenseTypeService, mock(ContactService::class.java), paymentMethodDataFetcher, userQualifier
        )
    }
}
