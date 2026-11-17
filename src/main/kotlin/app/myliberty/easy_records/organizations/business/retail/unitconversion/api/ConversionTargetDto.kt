package app.myliberty.easy_records.organizations.business.retail.unitconversion.api

import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.business.ConversionRatio
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import java.math.BigDecimal
import java.util.UUID

data class ConversionTargetDto(
    val id: UUID,
    val name: String,
    val code: String,
    val numerator: Long,
    val denominator: Long,
    val factor: BigDecimal = Decimals.Scale10.divide(numerator.toBigDecimal(), denominator.toBigDecimal())
) {

}

typealias ConversionTargets = Map<UUID, ConversionTargetDto>

data class UnitConversionGraph(
    private val fromUnitToTargets: Map<UUID, ConversionTargets>,
    private val unitNamesById: Map<UUID, String> = emptyMap(),
    private val placementByUnitId: Map<UUID, UnitPlacement> = emptyMap(),
    private val breakdownUnits: List<BreakdownUnit> = emptyList()
) {

    fun getBreakdownUnits(): List<BreakdownUnit> = breakdownUnits

    fun getStorageUnitId(unitId: UUID): UUID = placementOf(unitId).rootUnitId

    fun getSizeInRoot(unitId: UUID): Long = placementOf(unitId).sizeInRoot

    private fun placementOf(unitId: UUID): UnitPlacement =
        placementByUnitId[unitId] ?: throw RtsGenericException("Unit not found for id $unitId")

    fun getFullGraph(): Map<UUID, ConversionTargets> = fromUnitToTargets

    fun getTarget(fromUnitId: UUID, toUnitId: UUID): ConversionTargetDto {
        val fromName = label(fromUnitId)
        val targets = fromUnitToTargets[fromUnitId]
            ?: throw RtsGenericException("No conversion targets found for unit $fromName")
        return targets[toUnitId]
            ?: throw RtsGenericException("No conversion target found from $fromName to ${label(toUnitId)}")
    }

    fun getRatio(fromUnitId: UUID, toUnitId: UUID): ConversionRatio {
        if (fromUnitId == toUnitId) return ConversionRatio.IDENTITY
        val target = getTarget(fromUnitId, toUnitId)
        return ConversionRatio(target.numerator, target.denominator)
    }

    private fun label(unitId: UUID): String =
        unitNamesById[unitId] ?: throw RtsGenericException("Unit name not found for id $unitId")
}
