package app.myliberty.easy_records.organizations.business.unitgroup.api

import java.io.Serializable

data class UnitGroupInsertDto(
    val name: String,
    val description: String? = null,
    val baseUnitName: String? = null,
    val baseUnitCode: String? = null
) : Serializable
