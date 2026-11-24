package app.myliberty.easy_records.organizations.business.retail.product_category.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class ProductCategoryUpdateDtoTest {

    private val existing = ProductCategoryDto(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), "REF-1", "Dairy", "Milk and cheese")

    @Test
    fun `omitted fields keep the existing values`() {
        val updated = ProductCategoryUpdateDto(id = existing.id, categoryName = Optional.of("Cold Dairy")).applyTo(existing)

        assertEquals("Cold Dairy", updated.categoryName)
        assertEquals("Milk and cheese", updated.description)
    }

    @Test
    fun `an explicitly empty description clears the existing one`() {
        assertNull(ProductCategoryUpdateDto(id = existing.id, description = Optional.empty()).applyTo(existing).description)
    }
}
