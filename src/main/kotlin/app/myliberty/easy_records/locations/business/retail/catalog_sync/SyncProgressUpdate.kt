package app.myliberty.easy_records.locations.business.retail.catalog_sync

import java.util.UUID

data class SyncProgressUpdate(
  val syncLogId: UUID,
  val processed: Int,
  val skipped: Int,
  val failed: Int,
  val lastRevision: Long?
)
