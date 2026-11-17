package app.myliberty.easy_records.locations.business.retail.catalog_sync

import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncLogFetcher
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component


@Component
class SyncValidator(private val syncLogFetcher: SyncLogFetcher) {

  fun validateCanSync(tableName: TableName) {
    if (tableName.schemaLevel != SchemaLevel.ORGANIZATION) {
      throw RtsGenericException(
        "Only organization schema tables can be synced to location schema. " +
          "Table $tableName is at ${tableName.schemaLevel} level."
      )
    }

    if (syncLogFetcher.existsInProgressSync(tableName)) {
      val existingSync = syncLogFetcher.findInProgressSync(tableName)
      throw RtsGenericException(
        "Sync already in progress for table $tableName (syncLogId: ${existingSync?.id}). " +
          "Wait for it to complete or cancel it before starting a new sync."
      )
    }
  }
}
