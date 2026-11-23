package app.myliberty.easy_records.organizations.business.retail.product.api

import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class OrganizationProductUpdateDtoTest {

    private val existing = OrganizationProductDto(
        UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), "REF-1", "Whole Milk", "One litre",
        UUID.randomUUID(), "Milk", UUID.randomUUID(), ProductStatus.ACTIVE
    )

    @Test
    fun `omitted fields keep the existing values`() {
        val updated = OrganizationProductUpdateDto(id = existing.id, productName = Optional.of("Skim Milk")).applyTo(existing)

        assertEquals("Skim Milk", updated.productName)
        assertEquals("One litre", updated.description)
        assertEquals(existing.productGroupId, updated.productGroupId)
        assertEquals(existing.baseUnitId, updated.baseUnitId)
    }

    @Test
    fun `a provided group replaces the existing one`() {
        val newGroupId = UUID.randomUUID()

        val updated = OrganizationProductUpdateDto(id = existing.id, productGroupId = Optional.of(newGroupId)).applyTo(existing)

        assertEquals(newGroupId, updated.productGroupId)
    }

    @Test
    fun `an explicitly empty description clears the existing one`() {
        assertNull(OrganizationProductUpdateDto(id = existing.id, description = Optional.empty()).applyTo(existing).description)
    }
}
