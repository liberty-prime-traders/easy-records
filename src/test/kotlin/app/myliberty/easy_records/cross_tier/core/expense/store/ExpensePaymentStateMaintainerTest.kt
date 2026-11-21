package app.myliberty.easy_records.cross_tier.core.expense.store

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.entities.ExpensePaymentStateBase
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpensePaymentStateRepositoryBase
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// Search inner-joins the state table, so an expense without a state row vanishes from search and summary
// while every other read still returns it. refresh must surface that instead of skipping the expense.
class ExpensePaymentStateMaintainerTest {

    @Suppress("UNCHECKED_CAST")
    private val expensePaymentStateRepository =
        mock(ExpensePaymentStateRepositoryBase::class.java) as ExpensePaymentStateRepositoryBase<ExpensePaymentStateBase>

    @Test
    fun refreshFailsWhenAnExpenseHasNoPaymentStateRow() {
        val expenseDto = ExpenseDto(
            UUID.randomUUID(), "EXPN01", UUID.randomUUID(), "005.005", UUID.randomUUID(), BigDecimal("100"),
            LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
        )
        `when`(expensePaymentStateRepository.findByExpenseIdIn(setOf(expenseDto.id))).thenReturn(emptyList())

        assertThrows(LibertyGenericException::class.java) {
            ExpensePaymentStateMaintainer.refresh(expensePaymentStateRepository, listOf(expenseDto), mapOf(expenseDto.id to BigDecimal.ZERO))
        }
    }
}
