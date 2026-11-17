package app.myliberty.easy_records.platform.business.jurisdiction_type.api

import java.io.Serializable

data class JurisdictionTypeInsertDto(
    val name: String,
    val description: String? = null
) : Serializable
