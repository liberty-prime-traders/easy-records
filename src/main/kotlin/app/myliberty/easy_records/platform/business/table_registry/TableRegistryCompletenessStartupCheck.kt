package app.myliberty.easy_records.platform.business.table_registry

import app.myliberty.easy_records.platform.business.startup_checks.api.StartupCheck
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryService
import app.myliberty.easy_records.util.model.TableNames
import org.springframework.stereotype.Component

@Component
class TableRegistryCompletenessStartupCheck(
    private val tableRegistryService: TableRegistryService
) : StartupCheck {

    override val name = "table-registry-completeness"

    override fun check() {
        val registeredTableNames = tableRegistryService.getAllTableDtos().map { it.tableName }.toSet()
        val missing = tableNamesConstants() - registeredTableNames
        check(missing.isEmpty()) {
            "These tables are declared in TableNames but have no table_registry seed entry: " +
                "${missing.sorted()}. Add a table_registry insert changeset under registry-entries/."
        }
    }

    private fun tableNamesConstants(): Set<String> =
        TableNames::class.java.declaredFields
            .filter { it.type == String::class.java }
            .onEach { it.isAccessible = true }
            .map { it.get(null) as String }
            .toSet()
}
