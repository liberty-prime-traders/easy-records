package app.myliberty.easy_records.organizations.business.org_table_registry.mapping

import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TableNameResolver(private val tableRegistryService: TableRegistryService) {

    @TableNameQualifier
    fun resolveTableName(registryId: UUID?): String? {
        return registryId?.let {
            tableRegistryService.getAllTableDtos().find { it.id == registryId }?.tableName
        }
    }
}
