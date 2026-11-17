package app.myliberty.easy_records.locations.business.catalog_sync.strategy

import app.myliberty.easy_records.locations.business.catalog_sync.SyncCursor
import app.myliberty.easy_records.locations.business.catalog_sync.api.SyncMode

interface SyncStrategy {

  fun getSyncMode(): SyncMode

  fun shouldCalculateTotal(): Boolean

  fun createInitialCursor(resumableRevision: Long?): SyncCursor?

  fun getCursorForStorage(cursor: SyncCursor?): Long?
}
