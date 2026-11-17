package app.myliberty.easy_records.locations.business.catalog_sync.strategy

import app.myliberty.easy_records.locations.business.catalog_sync.api.SyncMode
import org.springframework.stereotype.Component


@Component
class SyncStrategyRegistry(strategies: List<SyncStrategy>) {

  private val strategyMap: Map<SyncMode, SyncStrategy> = strategies.associateBy { it.getSyncMode() }

  fun getStrategy(syncMode: SyncMode): SyncStrategy {
    return strategyMap[syncMode]
      ?: throw IllegalArgumentException("No strategy registered for sync mode: $syncMode")
  }
}
