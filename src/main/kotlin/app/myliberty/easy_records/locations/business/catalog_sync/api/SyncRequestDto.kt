package app.myliberty.easy_records.locations.business.catalog_sync.api

import app.myliberty.easy_records.util.model.TableName
import java.io.Serializable

data class SyncRequestDto(
  val tableName: TableName,
  val syncMode: SyncMode
) : Serializable
