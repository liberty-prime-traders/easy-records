package app.myliberty.easy_records.organizations.business.retail.unitgroup.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UnitGroupUpdateDtoTest {

    private val existing = UnitGroupDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-1",
        name = "Boxes",
        description = "Things sold in boxes"
    )

    @Test
    fun `an omitted description keeps the existing one`() {
        val updated = UnitGroupUpdateDto(id = existing.id, name = Optional.of("Crates")).applyTo(existing)

        assertEquals("Crates", updated.name)
        assertEquals("Things sold in boxes", updated.description)
    }

    @Test
    fun `an empty or blank description clears the existing one`() {
        assertNull(UnitGroupUpdateDto(id = existing.id, description = Optional.empty()).applyTo(existing).description)
        assertNull(UnitGroupUpdateDto(id = existing.id, description = Optional.of("   ")).applyTo(existing).description)
    }

    @Test
    fun `an omitted name keeps the existing one`() {
        val updated = UnitGroupUpdateDto(id = existing.id, description = Optional.of("New")).applyTo(existing)

        assertEquals("Boxes", updated.name)
        assertEquals("New", updated.description)
    }
}
