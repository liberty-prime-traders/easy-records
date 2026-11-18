package app.myliberty.easy_records.locations.business.retail.catalog_sync.strategy

import app.myliberty.easy_records.locations.business.retail.catalog_sync.SyncCursor
import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncMode
import org.springframework.stereotype.Component

@Component
class FullSyncStrategy : SyncStrategy {

  override fun getSyncMode(): SyncMode = SyncMode.FULL

  override fun shouldCalculateTotal(): Boolean = true

  override fun createInitialCursor(resumableRevision: Long?): SyncCursor? = null

  override fun getCursorForStorage(cursor: SyncCursor?): Long? = null
}
