package app.myliberty.easy_records.platform.business.table_registry.api

import app.myliberty.easy_records.util.enums.SchemaLevel
import java.io.Serializable
import java.util.UUID

data class TableRegistryResponseDto(
    val id: UUID,
    val tableName: String?,
    val defaultPrefix: String?,
    val minimumVersion: String?,
    val schemaLevel: SchemaLevel?,
    val displayName: String?,
    val description: String?,
    val userFacing: Boolean?,
    val validated: Boolean?
): Serializable
