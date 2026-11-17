package app.myliberty.easy_records.organizations.business.core.org_table_registry.api

import java.util.UUID

data class OrgTableRegistryDto(
    val id: UUID,
    val registryId: UUID,
    val defaultPrefix: String,
    val displayName: String
)
