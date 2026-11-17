package app.myliberty.easy_records.platform.business.jurisdiction.api

import java.io.Serializable
import java.util.UUID

data class JurisdictionInsertDto(
    val name: String,
    val jurisdictionTypeId: UUID,
    val parentJurisdictionId: UUID? = null,
    val taxTypesToAddOrReactivate: List<UUID>? = null
) : Serializable
