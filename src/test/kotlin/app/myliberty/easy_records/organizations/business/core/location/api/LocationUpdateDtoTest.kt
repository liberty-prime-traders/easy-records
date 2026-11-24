package app.myliberty.easy_records.organizations.business.core.location.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class LocationUpdateDtoTest {

    private val storedLocation = LocationDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "LOC-0001",
        locationType = LocationType.SHOP,
        name = "Test Location",
        description = "Front counter",
        schemaName = "loc_test_location",
        timezone = "Africa/Addis_Ababa"
    )

    @Test
    fun `a field left out of the update keeps the stored value`() {
        val locationUpdateDto = LocationUpdateDto(name = Optional.of("Cold Store"))

        val updated = locationUpdateDto.applyTo(storedLocation)

        assertEquals("Cold Store", updated.name)
        assertEquals("Front counter", updated.description)
        assertEquals("Africa/Addis_Ababa", updated.timezone)
        assertEquals(LocationType.SHOP, updated.locationType)
    }

    @Test
    fun `a provided type and timezone replace the stored values and the schema is never changed`() {
        val locationUpdateDto = LocationUpdateDto(
            locationType = Optional.of(LocationType.STORE),
            timezone = Optional.of("Europe/London")
        )

        val updated = locationUpdateDto.applyTo(storedLocation)

        assertEquals(LocationType.STORE, updated.locationType)
        assertEquals("Europe/London", updated.timezone)
        assertEquals("Test Location", updated.name)
        assertEquals("loc_test_location", updated.schemaName)
    }
}
