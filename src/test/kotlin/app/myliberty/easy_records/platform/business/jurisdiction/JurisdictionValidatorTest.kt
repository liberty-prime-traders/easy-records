package app.myliberty.easy_records.platform.business.jurisdiction

import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionDto
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class JurisdictionValidatorTest {

    private lateinit var validator: JurisdictionValidator

    private val province = jurisdictionDto("Province")
    private val region = jurisdictionDto("Region", parentJurisdictionId = province.id)

    @BeforeEach
    fun setUp() {
        val jurisdictionCache = mock(JurisdictionCache::class.java)
        val jurisdictionTypeService = mock(JurisdictionTypeService::class.java)
        `when`(jurisdictionCache.getAll()).thenReturn(listOf(province, region))
        `when`(jurisdictionTypeService.getAll()).thenReturn(emptyList())
        validator = JurisdictionValidator(jurisdictionCache, jurisdictionTypeService)
    }

    @Test
    fun `a jurisdiction name must have a value`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateName(" ")
        }
        assertEquals("Name must not be blank", exception.message)
    }

    @Test
    fun `a jurisdiction type must exist`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateTypeExists(UUID.randomUUID())
        }
        assertEquals("Jurisdiction type not found", exception.message)
    }

    @Test
    fun `a parent must exist`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateParent(UUID.randomUUID(), province.id)
        }
        assertEquals("Parent jurisdiction not found", exception.message)
    }

    @Test
    fun `a jurisdiction cannot be its own parent`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateParent(province.id, province.id)
        }
        assertEquals("A jurisdiction cannot be its own parent", exception.message)
    }

    @Test
    fun `a parent that descends from the jurisdiction is rejected with both names`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateParent(region.id, province.id)
        }
        assertEquals(
            "Circular hierarchy detected: Region cannot be set as parent of Province because it is a descendant of Province",
            exception.message,
        )
    }

    @Test
    fun `an ancestor of the jurisdiction is an allowed parent`() {
        assertDoesNotThrow { validator.validateParent(province.id, region.id) }
    }

    private fun jurisdictionDto(name: String, parentJurisdictionId: UUID? = null) = JurisdictionDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$name",
        name = name,
        jurisdictionTypeId = UUID.randomUUID(),
        parentJurisdictionId = parentJurisdictionId,
    )
}
