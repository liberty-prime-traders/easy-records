package app.myliberty.easy_records.organizations.business.unit_bulk_import

import app.myliberty.easy_records.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import app.myliberty.easy_records.organizations.business.unitgroup.api.SystemUnitGroup
import app.myliberty.easy_records.organizations.business.unitvalue.api.RootUnitDefinition
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitChainNode
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitChainResolver
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitValueResponseDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.util.UUID

internal object BulkUnitChainValidator {

    fun validateChainSizes(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingUnitCodes: ExistingUnitCodes,
        errors: MutableList<String>
    ) {
        val nodeIdByCode = buildMap {
            existingUnitValues.forEach { putIfAbsent(it.code, UUID.randomUUID()) }
            payloadGroups.forEach { group ->
                group.unitValues.forEach { unitValue -> unitValue.code?.let { putIfAbsent(it, UUID.randomUUID()) } }
                putIfAbsent(RootUnitDefinition.effectiveBaseUnitCode(group.name, group.baseUnitCode), UUID.randomUUID())
            }
        }
        val existingNodes = existingUnitValues.map { unitValue ->
            val baseUnitCode = existingUnitCodes.codeById[unitValue.baseUnit]
            UnitChainNode(nodeIdByCode.getValue(unitValue.code), baseUnitCode?.let { nodeIdByCode[it] }, unitValue.unitsOfBasePerUnit)
        }
        val existingNodeIds = existingNodes.map { it.id }.toSet()
        val payloadNodes = payloadGroups.flatMap { group ->
            val rootUnitCode = RootUnitDefinition.effectiveBaseUnitCode(group.name, group.baseUnitCode)
            val rootNode = UnitChainNode(nodeIdByCode.getValue(rootUnitCode), null, null)
            val unitNodes = group.unitValues
                .filter { it.code !in existingUnitCodes.codes }
                .map { unitValue ->
                    val baseUnitCode = if (unitValue.baseUnitCode == RootUnitDefinition.PIECE_BASE_UNIT_CODE)
                        rootUnitCode else unitValue.baseUnitCode
                    UnitChainNode(nodeIdByCode.getValue(unitValue.code!!), baseUnitCode?.let { nodeIdByCode[it] }, unitValue.unitsOfBasePerUnit)
                }
            if (SystemUnitGroup.isExcludedFromRootAutoInsert(group.name) || rootNode.id in existingNodeIds) unitNodes else listOf(rootNode) + unitNodes
        }
        try {
            UnitChainResolver.resolvePlacements(existingNodes + payloadNodes)
        } catch (chainException: RtsGenericException) {
            errors.add("Unit chain rejected: ${chainException.message}")
        }
    }

    fun validateNoCycles(
        payloadGroups: List<UnitGroupBulkInsertDto>,
        existingUnitValues: Collection<UnitValueResponseDto>,
        existingUnitCodes: ExistingUnitCodes,
        groupNameByCode: Map<String, String>,
        errors: MutableList<String>
    ) {
        val graph = UnitValueDependencyGraph()

        existingUnitValues.filter { !SystemUnitGroup.isMiscellaneous(groupNameByCode[it.code]) }.forEach { unitValue ->
            graph.addNode(unitValue.code)
            val baseUnitCode = existingUnitCodes.codeById[unitValue.baseUnit]
            if (baseUnitCode != null) graph.addEdge(baseUnitCode, unitValue.code)
        }

        payloadGroups.filter { !SystemUnitGroup.isMiscellaneous(it.name) }.forEach { group ->
            group.unitValues.filter { it.code !in existingUnitCodes.systemDefinedCodes }.forEach { unitValue ->
                val code = StringUtils.getValueOrNull(unitValue.code) ?: return@forEach
                graph.addNode(code)
                val baseUnitCode = unitValue.baseUnitCode
                if (baseUnitCode != null && baseUnitCode != RootUnitDefinition.PIECE_BASE_UNIT_CODE) {
                    graph.addEdge(baseUnitCode, code)
                }
            }
        }

        graph.findCycle()?.let { cycle -> errors.add("Cycle detected: ${cycle.joinToString(" -> ")}") }
    }
}
