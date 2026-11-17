package app.myliberty.easy_records.organizations.business.retail.unitvalue.api

import java.io.Serializable
import java.util.UUID

data class UnitValueInsertDto(
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val unitGroupId: UUID? = null,
    val baseUnit: UUID? = null,
    val unitsOfBasePerUnit: Long? = null
) : Serializable
