package app.myliberty.easy_records.organizations.business.core.location

import app.myliberty.easy_records.organizations.business.core.location.api.LocationUpdateDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.Optional

class LocationValidatorTest {

    private val locationCache: LocationCache = mock(LocationCache::class.java)
    private val locationValidator = LocationValidator(locationCache)

    @Test
    fun `an update that leaves the name out passes validation without a name check`() {
        val locationUpdateDto = LocationUpdateDto(description = Optional.of("Back room"))

        assertDoesNotThrow { locationValidator.validateLocationUpdate(locationUpdateDto) }
    }

    @Test
    fun `an explicit null name is rejected as a missing name`() {
        val locationUpdateDto = LocationUpdateDto(name = Optional.empty())

        val exception = assertThrows(LibertyGenericException::class.java) {
            locationValidator.validateLocationUpdate(locationUpdateDto)
        }

        assertEquals(LocationValidator.NAME_IS_REQUIRED, exception.message)
    }

    @Test
    fun `a blank name is rejected as a missing name`() {
        val locationUpdateDto = LocationUpdateDto(name = Optional.of("   "))

        val exception = assertThrows(LibertyGenericException::class.java) {
            locationValidator.validateLocationUpdate(locationUpdateDto)
        }

        assertEquals(LocationValidator.NAME_IS_REQUIRED, exception.message)
    }
}
