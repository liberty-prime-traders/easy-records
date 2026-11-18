package app.myliberty.easy_records.locations.business.retail.catalog_sync

sealed interface SyncCursor {
  data class Reference(val value: String) : SyncCursor
  data class Revision(val value: Long) : SyncCursor
}
