package app.myliberty.easy_records.organizations.business.retail.product_group.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class ProductGroupUpdateDtoTest {

    private val existing = ProductGroupDto(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), "REF-1", "Milk", "Fresh milk", UUID.randomUUID())

    @Test
    fun `omitted fields keep the existing values`() {
        val updated = ProductGroupUpdateDto(id = existing.id, groupName = Optional.of("Cold Milk")).applyTo(existing)

        assertEquals("Cold Milk", updated.groupName)
        assertEquals("Fresh milk", updated.description)
        assertEquals(existing.categoryId, updated.categoryId)
    }

    @Test
    fun `a provided category replaces the existing one`() {
        val newCategoryId = UUID.randomUUID()

        val updated = ProductGroupUpdateDto(id = existing.id, categoryId = Optional.of(newCategoryId)).applyTo(existing)

        assertEquals(newCategoryId, updated.categoryId)
        assertEquals("Milk", updated.groupName)
    }

    @Test
    fun `an explicitly empty description clears the existing one`() {
        assertNull(ProductGroupUpdateDto(id = existing.id, description = Optional.empty()).applyTo(existing).description)
    }
}
