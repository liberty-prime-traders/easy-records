package app.myliberty.easy_records.organizations.business.retail.unitvalue.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.SystemUnitGroup
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueCache
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueEntity
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueMapper
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueValidator
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitValueValidator: UnitValueValidator,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        lockUnitGroup(unitValueInsertDto.unitGroupId)
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val dto = unitValueCache.create(unitValueInsertDto)
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(dto, unitValueCache.getUnitNamesById()[dto.baseUnit])
    }

    fun bulkCreateValidatedList(topologicallyOrderedEntries: List<UnitValueBulkSaveEntry>): List<UnitValueResponseDto> {
        if (topologicallyOrderedEntries.isEmpty()) return emptyList()
        val entities = topologicallyOrderedEntries.map { entry ->
            UnitValueEntity(
                name = entry.name,
                description = entry.description,
                code = entry.code,
                unitGroupId = entry.unitGroupId,
                baseUnit = entry.baseUnit,
                unitsOfBasePerUnit = entry.unitsOfBasePerUnit,
                systemDefined = entry.systemDefined
            ).also { it.id = entry.id }
        }
        val saved = unitValueCache.saveAll(entities)
        unitConversionGraphFacade.invalidate()
        val unitNamesById = unitValueCache.getUnitNamesById()
        return saved.map {
            val dto = unitValueMapper.toDomainDto(it)
            unitValueMapper.toResponseDto(dto, unitNamesById[it.baseUnit])
        }
    }

    fun validateRootUnitCodeBeforeGroupCreate(unitGroupName: String?, baseUnitCode: String?) {
        if (SystemUnitGroup.isExcludedFromRootAutoInsert(unitGroupName)) return
        unitValueValidator.validateRootUnitCode(RootUnitDefinition.effectiveBaseUnitCode(unitGroupName, baseUnitCode))
    }

    // System groups are seeded with their own roots
    fun ensureBaseUnitExists(unitGroupId: UUID, unitGroupName: String?, baseUnitName: String?, baseUnitCode: String?): UnitValueResponseDto? {
        val excludedFromBaseUnit = SystemUnitGroup.isExcludedFromRootAutoInsert(unitGroupName)
        if (excludedFromBaseUnit) return null

        lockUnitGroup(unitGroupId)
        val rootUnitDefinition = RootUnitDefinition.forGroup(
            unitGroupId = unitGroupId,
            unitGroupName = unitGroupName,
            baseUnitName = baseUnitName,
            baseUnitCode = baseUnitCode
        )
        if (unitValueCache.getByUnitGroupId(unitGroupId).any { it.code == rootUnitDefinition.code }) return null
        unitValueValidator.validateRootUnitInsert(unitGroupId, rootUnitDefinition.name, rootUnitDefinition.code)

        val saved = unitValueCache.save(
            UnitValueEntity(
                name = rootUnitDefinition.name,
                description = rootUnitDefinition.description,
                code = rootUnitDefinition.code,
                unitGroupId = unitGroupId,
                systemDefined = rootUnitDefinition.systemDefined
            )
        )
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(saved, null)
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        val existing = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
            ?: throw UpdatingNonExistingRecordException()
        lockUnitGroup(existing.unitGroupId)
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        if (existing.systemDefined) throw RtsGenericException("System-defined unit values cannot be modified")
        val updated = unitValueUpdateDto.applyTo(existing)
        val saved = unitValueCache.save(updated)
        unitConversionGraphFacade.invalidate()
        return unitValueMapper.toResponseDto(saved, unitValueCache.getUnitNamesById()[saved.baseUnit])
    }

    fun deleteUnitValue(id: UUID?) {
        val allUnitValues = unitValueCache.getAllUnitValues()
        allUnitValues
            .find { it.id == id }
            ?.apply {
                if (systemDefined) throw RtsGenericException("System-defined unit values cannot be deleted")
                if (allUnitValues.any { it.baseUnit == id }) throw RtsGenericException(UNIT_IS_BASE_OF_ANOTHER_UNIT)
                unitValueCache.deleteUnitValue(id)
                unitConversionGraphFacade.invalidate()
            }
    }

    fun deleteAllInUnitGroup(unitGroupId: UUID) {
        lockUnitGroup(unitGroupId)
        val unitValuesInGroup = unitValueCache.getByUnitGroupId(unitGroupId)
        if (unitValuesInGroup.any { !it.systemDefined }) throw RtsGenericException(UNIT_GROUP_HAS_USER_DEFINED_UNITS)
        if (unitValuesInGroup.isEmpty()) return
        unitValueCache.deleteAllByIds(unitValuesInGroup.map { it.id })
        unitConversionGraphFacade.invalidate()
    }

    private fun lockUnitGroup(unitGroupId: UUID?) {
        unitGroupId?.let { orgEntityAdvisoryLock.acquire(LockNamespaces.UNIT_GROUP, it.toString()) }
    }

    companion object {
        const val UNIT_IS_BASE_OF_ANOTHER_UNIT = "A unit that is the base unit of another unit cannot be deleted"
        const val UNIT_GROUP_HAS_USER_DEFINED_UNITS =
            "A unit group with user-defined units cannot be deleted; delete those units first"
    }
}
