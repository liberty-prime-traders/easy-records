package app.myliberty.easy_records.organizations.business.retail.stock_item_source

import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`

class StockItemSourceSeederTest {

    @Test
    fun `seeding an empty organization inserts every stock item source with its display name`() {
        val stockItemSourceRepository = mock(StockItemSourceRepository::class.java)
        `when`(stockItemSourceRepository.findAll()).thenReturn(emptyList())

        StockItemSourceSeeder(stockItemSourceRepository).seed()

        val insertedEntities = savedEntities(stockItemSourceRepository)
        assertEquals(StockItemSource.entries.toSet(), insertedEntities.map { it.code }.toSet())
        insertedEntities.forEach { entity ->
            assertEquals(entity.code.displayName, entity.name)
            assertTrue(entity.systemDefined)
        }
    }

    @Test
    fun `re-seeding an organization that already holds the stock item sources writes nothing`() {
        val firstRepository = mock(StockItemSourceRepository::class.java)
        `when`(firstRepository.findAll()).thenReturn(emptyList())
        StockItemSourceSeeder(firstRepository).seed()
        val seededEntities = savedEntities(firstRepository)

        val secondRepository = mock(StockItemSourceRepository::class.java)
        `when`(secondRepository.findAll()).thenReturn(seededEntities)
        StockItemSourceSeeder(secondRepository).seed()

        assertTrue(savedEntities(secondRepository).isEmpty())
    }

    private fun savedEntities(stockItemSourceRepository: StockItemSourceRepository): List<StockItemSourceEntity> =
        mockingDetails(stockItemSourceRepository).invocations
            .filter { it.method.name == "saveAll" }
            .flatMap { invocation -> invocation.arguments.first() as Collection<*> }
            .map { it as StockItemSourceEntity }
}
