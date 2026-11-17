package app.myliberty.easy_records.locations.business.catalog_sync.api

import app.myliberty.easy_records.locations.business.catalog_sync.SyncCursor
import app.myliberty.easy_records.util.model.TableName
import java.util.UUID

interface SyncService<T> {

  val tableName: TableName

  fun supports(requestedTableName: TableName): Boolean = tableName == requestedTableName

  fun countAllRecords(): Int

  fun fetchBatch(cursor: SyncCursor?, batchSize: Int): List<T>

  fun createLocationRecord(record: T): Boolean

  fun extractCursor(record: T): SyncCursor

  fun syncSingle(entityId: UUID)
}
