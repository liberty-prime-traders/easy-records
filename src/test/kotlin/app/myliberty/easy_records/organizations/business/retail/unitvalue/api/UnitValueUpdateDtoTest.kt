package app.myliberty.easy_records.organizations.business.retail.unitvalue.api

import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UnitValueUpdateDtoTest {

    private val existing = UnitValueDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-1",
        name = "Dozen",
        code = "doz",
        description = "Twelve pieces",
        baseUnit = UUID.randomUUID(),
        unitsOfBasePerUnit = 12L
    )

    @Test
    fun `omitted fields keep the existing values`() {
        val updated = UnitValueUpdateDto(id = existing.id, name = Optional.of("Dz")).applyTo(existing)

        assertEquals("Dz", updated.name)
        assertEquals("doz", updated.code)
        assertEquals("Twelve pieces", updated.description)
        assertEquals(existing.baseUnit, updated.baseUnit)
        assertEquals(12L, updated.unitsOfBasePerUnit)
    }

    @Test
    fun `an empty description clears the existing one`() {
        assertNull(UnitValueUpdateDto(id = existing.id, description = Optional.empty()).applyTo(existing).description)
    }

    // An empty Optional keeps the base unit, so a chain unit can never be turned into a root by an update.
    @Test
    fun `an empty base unit keeps the existing one`() {
        val updated = UnitValueUpdateDto(id = existing.id, baseUnit = Optional.empty()).applyTo(existing)

        assertEquals(existing.baseUnit, updated.baseUnit)
    }
}
