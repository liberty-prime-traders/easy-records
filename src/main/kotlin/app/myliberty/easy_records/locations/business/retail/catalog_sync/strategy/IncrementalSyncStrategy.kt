package app.myliberty.easy_records.locations.business.retail.catalog_sync.strategy

import app.myliberty.easy_records.locations.business.retail.catalog_sync.SyncCursor
import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncMode
import org.springframework.stereotype.Component

@Component
class IncrementalSyncStrategy : SyncStrategy {

  override fun getSyncMode(): SyncMode = SyncMode.INCREMENTAL

  override fun shouldCalculateTotal(): Boolean = false

  override fun createInitialCursor(resumableRevision: Long?): SyncCursor? {
    return SyncCursor.Revision(resumableRevision ?: 0L)
  }

  override fun getCursorForStorage(cursor: SyncCursor?): Long? {
    return (cursor as? SyncCursor.Revision)?.value
  }
}
