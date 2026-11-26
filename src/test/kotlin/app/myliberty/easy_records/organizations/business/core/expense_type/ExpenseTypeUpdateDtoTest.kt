package app.myliberty.easy_records.organizations.business.core.expense_type

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeUpdateDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class ExpenseTypeUpdateDtoTest {

    private val existingExpenseTypeDto = ExpenseTypeDto(
        id = UUID.fromString("0f8c2d1e-6a4b-4c3d-9e2f-1a2b3c4d5e6f"),
        createdById = UUID.fromString("1f8c2d1e-6a4b-4c3d-9e2f-1a2b3c4d5e6f"),
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        code = null,
        name = "Fleet Fuel",
        expenseAccountCode = "005.009",
        eligiblePayeeTypes = setOf(ContactType.SUPPLIER),
        eligibleSourceTypes = setOf(ExpenseSourceType.ADHOC),
        systemDefined = false
    )

    @Test
    fun `fields that are not provided keep the stored values`() {
        val expenseTypeUpdateDto = ExpenseTypeUpdateDto(id = existingExpenseTypeDto.id)

        val updatedExpenseTypeDto = expenseTypeUpdateDto.applyTo(existingExpenseTypeDto)

        assertEquals(existingExpenseTypeDto, updatedExpenseTypeDto)
    }

    @Test
    fun `an explicitly empty eligibility set replaces the stored one so the service can reject it`() {
        val expenseTypeUpdateDto = ExpenseTypeUpdateDto(
            id = existingExpenseTypeDto.id,
            eligiblePayeeTypes = Optional.of(emptySet())
        )

        val updatedExpenseTypeDto = expenseTypeUpdateDto.applyTo(existingExpenseTypeDto)

        assertEquals(emptySet<ContactType>(), updatedExpenseTypeDto.eligiblePayeeTypes)
        assertEquals(existingExpenseTypeDto.eligibleSourceTypes, updatedExpenseTypeDto.eligibleSourceTypes)
    }

    @Test
    fun `a provided name and account replace only those fields`() {
        val expenseTypeUpdateDto = ExpenseTypeUpdateDto(
            id = existingExpenseTypeDto.id,
            name = Optional.of("Fleet Costs"),
            expenseAccountCode = Optional.of("005.007")
        )

        val updatedExpenseTypeDto = expenseTypeUpdateDto.applyTo(existingExpenseTypeDto)

        assertEquals("Fleet Costs", updatedExpenseTypeDto.name)
        assertEquals("005.007", updatedExpenseTypeDto.expenseAccountCode)
        assertEquals(existingExpenseTypeDto.eligiblePayeeTypes, updatedExpenseTypeDto.eligiblePayeeTypes)
    }
}
