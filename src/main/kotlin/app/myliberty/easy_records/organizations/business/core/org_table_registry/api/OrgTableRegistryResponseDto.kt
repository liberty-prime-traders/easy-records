package app.myliberty.easy_records.organizations.business.core.org_table_registry.api

import java.io.Serializable
import java.util.UUID

data class OrgTableRegistryResponseDto(
    val id: UUID,
    val registryId: UUID?,
    val tableName: String?,
    val defaultPrefix: String?,
    val displayName: String?
): Serializable
