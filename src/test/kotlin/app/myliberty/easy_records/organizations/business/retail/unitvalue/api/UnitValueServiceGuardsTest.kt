package app.myliberty.easy_records.organizations.business.retail.unitvalue.api

import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueCache
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueDto
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueMapper
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueValidator
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UnitValueServiceGuardsTest {

    private val unitGroupId = UUID.randomUUID()
    private val gram = unit("Gram", "g", systemDefined = true)
    private val kilogram = unit("Kilogram", "kg", baseUnit = gram.id, unitsOfBasePerUnit = 1000L)
    private val tonne = unit("Tonne", "t", baseUnit = kilogram.id, unitsOfBasePerUnit = 1000L)

    private lateinit var unitValueCache: UnitValueCache
    private lateinit var unitConversionGraphFacade: UnitConversionGraphFacade
    private lateinit var service: UnitValueService

    @BeforeEach
    fun setUp() {
        unitValueCache = mock(UnitValueCache::class.java)
        unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
        service = UnitValueService(
            unitValueCache,
            mock(UnitValueMapper::class.java),
            mock(UnitValueValidator::class.java),
            unitConversionGraphFacade,
            mock(OrgEntityAdvisoryLock::class.java)
        )
        `when`(unitValueCache.getAllUnitValues()).thenReturn(listOf(gram, kilogram, tonne))
        `when`(unitValueCache.getByUnitGroupId(unitGroupId)).thenReturn(listOf(gram, kilogram, tonne))
    }

    @Test
    fun `updating an unknown unit value is rejected`() {
        assertThrows(UpdatingNonExistingRecordException::class.java) {
            service.updateUnitValue(UnitValueUpdateDto(id = UUID.randomUUID(), name = Optional.of("Pound"), code = Optional.of("lb")))
        }
    }

    @Test
    fun `a system-defined unit value cannot be updated`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.updateUnitValue(UnitValueUpdateDto(id = gram.id, name = Optional.of("Grams"), code = Optional.of("g")))
        }

        assertEquals("System-defined unit values cannot be modified", exception.message)
        verify(unitValueCache, never()).save(anyNonNull(UnitValueDto::class.java))
    }

    @Test
    fun `a system-defined unit value cannot be deleted`() {
        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteUnitValue(gram.id) }

        assertEquals("System-defined unit values cannot be deleted", exception.message)
        verify(unitValueCache, never()).deleteUnitValue(gram.id)
    }

    @Test
    fun `a unit that is the base of another unit cannot be deleted`() {
        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteUnitValue(kilogram.id) }

        assertEquals(UnitValueService.UNIT_IS_BASE_OF_ANOTHER_UNIT, exception.message)
        verify(unitValueCache, never()).deleteUnitValue(kilogram.id)
    }

    @Test
    fun `deleting a leaf unit removes it and invalidates the conversion graph`() {
        service.deleteUnitValue(tonne.id)

        verify(unitValueCache).deleteUnitValue(tonne.id)
        verify(unitConversionGraphFacade).invalidate()
    }

    @Test
    fun `a unit group with a user-defined unit keeps all of its units`() {
        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteAllInUnitGroup(unitGroupId) }

        assertEquals(UnitValueService.UNIT_GROUP_HAS_USER_DEFINED_UNITS, exception.message)
        verify(unitValueCache, never()).deleteAllByIds(anyCollection())
    }

    @Test
    fun `a unit group holding only its system-defined root has the root removed`() {
        `when`(unitValueCache.getByUnitGroupId(unitGroupId)).thenReturn(listOf(gram))

        service.deleteAllInUnitGroup(unitGroupId)

        verify(unitValueCache).deleteAllByIds(listOf(gram.id))
        verify(unitConversionGraphFacade).invalidate()
    }

    @Test
    fun `an empty unit group needs no unit deletion`() {
        `when`(unitValueCache.getByUnitGroupId(unitGroupId)).thenReturn(emptyList())

        service.deleteAllInUnitGroup(unitGroupId)

        verify(unitValueCache, never()).deleteAllByIds(anyCollection())
        verify(unitConversionGraphFacade, never()).invalidate()
    }

    private fun unit(
        name: String,
        code: String,
        baseUnit: UUID? = null,
        unitsOfBasePerUnit: Long? = null,
        systemDefined: Boolean = false
    ) = UnitValueDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$code",
        name = name,
        code = code,
        unitGroupId = unitGroupId,
        baseUnit = baseUnit,
        unitsOfBasePerUnit = unitsOfBasePerUnit,
        systemDefined = systemDefined
    )
}
