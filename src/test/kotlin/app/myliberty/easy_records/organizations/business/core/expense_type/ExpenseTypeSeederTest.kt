package app.myliberty.easy_records.organizations.business.core.expense_type

import app.myliberty.easy_records.organizations.business.core.expense_type.api.SystemExpenseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`

class ExpenseTypeSeederTest {

    @Test
    fun `seeding an empty organization inserts every system expense type against its expense account`() {
        val expenseTypeRepository = mock(ExpenseTypeRepository::class.java)
        `when`(expenseTypeRepository.findAll()).thenReturn(emptyList())

        ExpenseTypeSeeder(expenseTypeRepository).seed()

        val insertedEntities = savedEntities(expenseTypeRepository)
        assertEquals(SystemExpenseType.entries.map { it.code }.toSet(), insertedEntities.map { it.code }.toSet())
        insertedEntities.forEach { entity ->
            val systemExpenseType = SystemExpenseType.entries.first { it.code == entity.code }
            assertEquals(systemExpenseType.expenseAccount.code, entity.expenseAccountCode, systemExpenseType.name)
            assertTrue(entity.systemDefined)
        }
    }

    @Test
    fun `re-seeding an organization that already holds the system expense types writes nothing`() {
        val firstRepository = mock(ExpenseTypeRepository::class.java)
        `when`(firstRepository.findAll()).thenReturn(emptyList())
        ExpenseTypeSeeder(firstRepository).seed()
        val seededEntities = savedEntities(firstRepository)

        val secondRepository = mock(ExpenseTypeRepository::class.java)
        `when`(secondRepository.findAll()).thenReturn(seededEntities)
        ExpenseTypeSeeder(secondRepository).seed()

        assertTrue(savedEntities(secondRepository).isEmpty())
    }

    private fun savedEntities(expenseTypeRepository: ExpenseTypeRepository): List<ExpenseTypeEntity> =
        mockingDetails(expenseTypeRepository).invocations
            .filter { it.method.name == "saveAll" }
            .flatMap { invocation -> invocation.arguments.first() as Collection<*> }
            .map { it as ExpenseTypeEntity }
}
