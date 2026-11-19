package app.myliberty.easy_records.locations.business.retail.catalog_sync.api

import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component

@Component
class SyncServiceRegistry(
  private val syncServices: List<SyncService<*>>
) {

  fun getService(tableName: TableName): SyncService<*> {
    return syncServices.firstOrNull { it.supports(tableName) }
      ?: throw IllegalArgumentException("No sync service registered for table: $tableName")
  }
}
