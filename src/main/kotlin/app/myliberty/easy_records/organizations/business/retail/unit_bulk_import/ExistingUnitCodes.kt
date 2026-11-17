package app.myliberty.easy_records.organizations.business.retail.unit_bulk_import

import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueResponseDto
import java.util.UUID

internal class ExistingUnitCodes(existingUnitValues: Collection<UnitValueResponseDto>) {
    val codes: Set<String> = existingUnitValues.map { it.code }.toSet()
    val systemDefinedCodes: Set<String> = existingUnitValues.filter { it.systemDefined }.map { it.code }.toSet()
    val codeById: Map<UUID, String> = existingUnitValues.associate { it.id to it.code }
}
