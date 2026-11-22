package app.myliberty.easy_records.organizations.business.retail.unitgroup.api

import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.retail.unitgroup.UnitGroupCache
import app.myliberty.easy_records.organizations.business.retail.unitgroup.UnitGroupMapper
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueService
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UnitGroupServiceUpdateDeleteTest {

    private lateinit var unitGroupCache: UnitGroupCache
    private lateinit var unitGroupMapper: UnitGroupMapper
    private lateinit var unitValueService: UnitValueService
    private lateinit var service: UnitGroupService

    private val packSize = unitGroupDto("Pack Size")
    private val weight = unitGroupDto("Weight", systemDefined = true)

    @BeforeEach
    fun setUp() {
        unitGroupCache = mock(UnitGroupCache::class.java)
        unitGroupMapper = mock(UnitGroupMapper::class.java)
        unitValueService = mock(UnitValueService::class.java)
        service = UnitGroupService(unitGroupMapper, unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java))
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(packSize, weight))
    }

    @Test
    fun `updating an unknown unit group is rejected`() {
        assertThrows(UpdatingNonExistingRecordException::class.java) {
            service.updateUnitGroup(UnitGroupUpdateDto(id = UUID.randomUUID(), name = Optional.of("Boxes")))
        }
    }

    @Test
    fun `a system-defined unit group cannot be updated`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.updateUnitGroup(UnitGroupUpdateDto(id = weight.id, name = Optional.of("Heavy Things")))
        }

        assertEquals("System-defined unit groups cannot be modified", exception.message)
        verify(unitGroupCache, never()).save(anyNonNull(UnitGroupDto::class.java))
    }

    @Test
    fun `a unit group can be renamed to a spelling of its own name`() {
        val renamed = packSize.copy(name = "pack-size")
        stubSave(renamed)

        service.updateUnitGroup(UnitGroupUpdateDto(id = packSize.id, name = Optional.of("pack-size")))

        verify(unitGroupCache).save(renamed)
    }

    @Test
    fun `a unit group cannot be renamed to another group's name`() {
        val boxes = unitGroupDto("Boxes")
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(packSize, boxes, weight))

        assertThrows(LibertyGenericException::class.java) {
            service.updateUnitGroup(UnitGroupUpdateDto(id = boxes.id, name = Optional.of("Pack-Size")))
        }
    }

    // applyTo keeps omitted fields, so a description-only update must pass without a name. Requiring one fails every
    // partial update.
    @Test
    fun `an update carrying only a description keeps the existing name`() {
        val described = packSize.copy(description = "Things sold in packs")
        stubSave(described)

        service.updateUnitGroup(UnitGroupUpdateDto(id = packSize.id, description = Optional.of("Things sold in packs")))

        verify(unitGroupCache).save(described)
    }

    @Test
    fun `a system-defined unit group cannot be deleted`() {
        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteUnitGroup(weight.id) }

        assertEquals("System-defined unit groups cannot be deleted", exception.message)
        verify(unitGroupCache, never()).deleteUnitGroup(weight.id)
    }

    @Test
    fun `deleting a unit group removes its units before the group row`() {
        service.deleteUnitGroup(packSize.id)

        val order = inOrder(unitValueService, unitGroupCache)
        order.verify(unitValueService).deleteAllInUnitGroup(packSize.id)
        order.verify(unitGroupCache).deleteUnitGroup(packSize.id)
    }

    @Test
    fun `a unit group whose units cannot be removed is kept`() {
        `when`(unitValueService.deleteAllInUnitGroup(packSize.id))
            .thenThrow(LibertyGenericException(UnitValueService.UNIT_GROUP_HAS_USER_DEFINED_UNITS))

        assertThrows(LibertyGenericException::class.java) { service.deleteUnitGroup(packSize.id) }

        verify(unitGroupCache, never()).deleteUnitGroup(packSize.id)
    }

    private fun stubSave(saved: UnitGroupDto) {
        `when`(unitGroupCache.save(saved)).thenReturn(saved)
        `when`(unitGroupMapper.toResponseDto(saved)).thenReturn(
            UnitGroupResponseDto(saved.id, "Someone", saved.createdOn, saved.name, saved.description, saved.referenceNumber, saved.systemDefined)
        )
    }

    private fun unitGroupDto(name: String, systemDefined: Boolean = false) = UnitGroupDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$name",
        name = name,
        systemDefined = systemDefined
    )
}
