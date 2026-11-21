package app.myliberty.easy_records.configuration.session

import java.util.UUID

data class OrgSession(
    val id: UUID,
    val schemaName: String,
    val timezone: String? = null
)
