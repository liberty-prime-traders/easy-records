package app.myliberty.easy_records.organizations.business.retail.unitvalue

import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupDataFetcher
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.RootUnitDefinition
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.SystemUnitValue
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitChainNode
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitChainResolver
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueInsertDto
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueUpdateDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitValueValidator(
    private val unitValueCache: UnitValueCache,
    private val unitGroupDataFetcher: UnitGroupDataFetcher
) {

    fun validateUnitValueInsert(unitValueInsertDto: UnitValueInsertDto) {
        if (!StringUtils.hasValue(unitValueInsertDto.name)) {
            throw LibertyGenericException(NAME_IS_REQUIRED)
        }
        if (!StringUtils.hasValue(unitValueInsertDto.code)) {
            throw LibertyGenericException(CODE_IS_REQUIRED)
        }
        if (unitValueInsertDto.unitGroupId == null) {
            throw LibertyGenericException(UNIT_GROUP_ID_IS_REQUIRED)
        }
        if (!unitGroupDataFetcher.exists(unitValueInsertDto.unitGroupId)) {
            throw LibertyGenericException(PROVIDED_MISSING_UNIT_GROUP)
        }
        requireNameIsFreeInGroup(unitValueInsertDto.name!!, unitValueInsertDto.unitGroupId, excludedUnitId = null)
        requireCodeIsWellFormed(unitValueInsertDto.code!!)
        requireCodeIsFree(unitValueInsertDto.code, excludedUnitId = null)

        if (unitGroupDataFetcher.isMiscellaneous(unitValueInsertDto.unitGroupId)) {
            if (unitValueInsertDto.baseUnit != null || unitValueInsertDto.unitsOfBasePerUnit != null) {
                throw LibertyGenericException(MISCELLANEOUS_UNIT_CANNOT_HAVE_BASE_UNIT)
            }
            return
        }
        if (unitValueInsertDto.baseUnit == null) {
            throw LibertyGenericException(BASE_UNIT_IS_REQUIRED)
        }
        if (unitValueInsertDto.unitsOfBasePerUnit == null) {
            throw LibertyGenericException(UNITS_OF_BASE_PER_UNIT_IS_REQUIRED)
        }
        requireFactorWithinBounds(unitValueInsertDto.unitsOfBasePerUnit)

        val unitGroupNodes = unitValueCache.getByUnitGroupId(unitValueInsertDto.unitGroupId).map { toChainNode(it) }
        if (unitGroupNodes.none { it.id == unitValueInsertDto.baseUnit }) {
            throw LibertyGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }
        val insertedNode = UnitChainNode(UUID.randomUUID(), unitValueInsertDto.baseUnit, unitValueInsertDto.unitsOfBasePerUnit)
        UnitChainResolver.resolvePlacements(unitGroupNodes + insertedNode)
    }

    // The auto-inserted root has no base unit, so only the name and code can collide
    fun validateRootUnitInsert(unitGroupId: UUID, name: String, code: String) {
        requireNameIsFreeInGroup(name, unitGroupId, excludedUnitId = null)
        validateRootUnitCode(code)
    }

    fun validateRootUnitCode(code: String) {
        requireCodeIsWellFormed(code)
        requireCodeIsFree(code, excludedUnitId = null)
    }

    fun validateUnitValueUpdate(unitValueUpdateDto: UnitValueUpdateDto) {
        val name = unitValueUpdateDto.name?.get()
        if (!StringUtils.hasValue(name)) {
            throw LibertyGenericException(NAME_IS_REQUIRED)
        }
        val code = unitValueUpdateDto.code?.get()
        if (!StringUtils.hasValue(code)) {
            throw LibertyGenericException(CODE_IS_REQUIRED)
        }
        val existing = unitValueCache.getAllUnitValues().find { it.id == unitValueUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()

        requireNameIsFreeInGroup(name!!, existing.unitGroupId, excludedUnitId = existing.id)
        requireCodeIsWellFormed(code!!)
        requireCodeIsFree(code, excludedUnitId = existing.id)

        val baseUnitIsProvided = unitValueUpdateDto.baseUnit?.isPresent == true
        val unitsOfBasePerUnitIsProvided = unitValueUpdateDto.unitsOfBasePerUnit?.isPresent == true

        if (baseUnitIsProvided && !unitsOfBasePerUnitIsProvided) {
            throw LibertyGenericException(UNITS_OF_BASE_PER_UNIT_IS_REQUIRED)
        }
        if (unitsOfBasePerUnitIsProvided && !baseUnitIsProvided) {
            throw LibertyGenericException(BASE_UNIT_IS_REQUIRED)
        }
        if (existing.baseUnit == null && baseUnitIsProvided) {
            throw LibertyGenericException(ROOT_UNIT_CANNOT_HAVE_BASE_UNIT)
        }
        if (unitsOfBasePerUnitIsProvided) {
            requireFactorWithinBounds(unitValueUpdateDto.unitsOfBasePerUnit.get())
        }

        val updated = unitValueUpdateDto.applyTo(existing)
        val unitGroupNodes = unitValueCache.getByUnitGroupId(existing.unitGroupId)
        if (baseUnitIsProvided && unitGroupNodes.none { it.id == updated.baseUnit }) {
            throw LibertyGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }
        val nodesAfterUpdate = unitGroupNodes.map { toChainNode(if (it.id == existing.id) updated else it) }
        UnitChainResolver.resolvePlacements(nodesAfterUpdate)
    }

    private fun toChainNode(unitValueDto: UnitValueDto): UnitChainNode =
        UnitChainNode(unitValueDto.id, unitValueDto.baseUnit, unitValueDto.unitsOfBasePerUnit)

    private fun requireNameIsFreeInGroup(name: String, unitGroupId: UUID?, excludedUnitId: UUID?) {
        StringUtils.requireLetterOrDigit(name, NAME_MUST_CONTAIN_LETTER_OR_DIGIT)
        unitValueCache.getByUnitGroupId(unitGroupId)
            .find { it.id != excludedUnitId && StringUtils.isEquivalentOnAlphanumerics(it.name, name) }
            ?.let { throw LibertyGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    private fun requireCodeIsWellFormed(code: String) {
        if (code.length > SystemUnitValue.MAX_CODE_LENGTH) {
            throw LibertyGenericException(String.format(CODE_TOO_LONG, SystemUnitValue.MAX_CODE_LENGTH))
        }
        if (code == RootUnitDefinition.PIECE_BASE_UNIT_CODE) {
            throw LibertyGenericException(String.format(CODE_IS_RESERVED, code))
        }
    }

    private fun requireCodeIsFree(code: String, excludedUnitId: UUID?) {
        unitValueCache.getAllUnitValues().find { it.code == code && it.id != excludedUnitId }
            ?.let { throw LibertyGenericException(String.format(CODE_ALREADY_EXISTS, code)) }
    }

    private fun requireFactorWithinBounds(unitsOfBasePerUnit: Long) {
        if (unitsOfBasePerUnit < 1) {
            throw LibertyGenericException(UNITS_OF_BASE_PER_UNIT_MUST_BE_POSITIVE)
        }
        if (unitsOfBasePerUnit > UnitChainResolver.MAX_SIZE_IN_ROOT) {
            throw LibertyGenericException(UnitChainResolver.CHAIN_SIZE_TOO_LARGE)
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A unit value must have a name"
        const val NAME_MUST_CONTAIN_LETTER_OR_DIGIT = "A unit value name must contain at least one letter or digit"
        const val CODE_IS_REQUIRED = "A unit code must have a name"
        const val UNIT_GROUP_ID_IS_REQUIRED = "A unit value must have a unit group id"
        const val UNITS_OF_BASE_PER_UNIT_IS_REQUIRED = "A unit value with a base unit must have unitsOfBasePerUnit"
        const val BASE_UNIT_IS_REQUIRED = "A unit value in this group must have a base unit and unitsOfBasePerUnit"
        const val UNITS_OF_BASE_PER_UNIT_MUST_BE_POSITIVE = "unitsOfBasePerUnit must be a positive whole number"
        const val PROVIDED_MISSING_UNIT_GROUP = "UnitGroup with the provided id does not exist"
        const val NAME_ALREADY_EXISTS = "A unit value with the name %s already exists"
        const val CODE_ALREADY_EXISTS = "A unit value with the code %s already exists"
        const val CODE_TOO_LONG = "A unit value code must be at most %d characters"
        const val CODE_IS_RESERVED = "The unit value code %s is reserved"
        const val BASE_UNIT_MUST_BE_IN_GROUP = "The base unit must be selected from the assigned group"
        const val MISCELLANEOUS_UNIT_CANNOT_HAVE_BASE_UNIT = "A Miscellaneous unit stands alone and cannot have a base unit"
        const val ROOT_UNIT_CANNOT_HAVE_BASE_UNIT = "A group's root unit cannot be given a base unit"
    }
}
