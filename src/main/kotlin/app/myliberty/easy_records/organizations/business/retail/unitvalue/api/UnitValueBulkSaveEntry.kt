package app.myliberty.easy_records.organizations.business.retail.unitvalue.api

import java.util.UUID

data class UnitValueBulkSaveEntry(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String?,
    val unitGroupId: UUID,
    val baseUnit: UUID?,
    val unitsOfBasePerUnit: Long?,
    val systemDefined: Boolean = false
)
