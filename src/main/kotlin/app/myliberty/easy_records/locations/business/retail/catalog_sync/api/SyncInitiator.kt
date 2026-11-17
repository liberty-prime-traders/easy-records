package app.myliberty.easy_records.locations.business.catalog_sync.api

import app.myliberty.easy_records.locations.business.catalog_sync.SyncLogEntity
import app.myliberty.easy_records.locations.business.catalog_sync.SyncLogMapper
import app.myliberty.easy_records.locations.business.catalog_sync.SyncOrchestrator
import app.myliberty.easy_records.locations.business.catalog_sync.SyncValidator
import app.myliberty.easy_records.locations.business.catalog_sync.strategy.SyncStrategyRegistry
import app.myliberty.easy_records.util.async.AsyncExecutor
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Service

@Service
class SyncInitiator(
  private val validator: SyncValidator,
  private val syncLogUpdater: SyncLogUpdater,
  private val strategyRegistry: SyncStrategyRegistry,
  private val syncServiceRegistry: SyncServiceRegistry,
  private val syncOrchestrator: SyncOrchestrator,
  private val asyncExecutor: AsyncExecutor,
  private val syncLogMapper: SyncLogMapper
) {

  fun initiate(tableName: TableName, syncMode: SyncMode, initiateType: SyncInitiateType): SyncLogResponseDto {
    validator.validateCanSync(tableName)
    val syncStrategy = strategyRegistry.getStrategy(syncMode)
    val syncService = syncServiceRegistry.getService(tableName)

    val totalRecords = if (syncStrategy.shouldCalculateTotal()) { syncService.countAllRecords() } else { null }

    val syncLog = SyncLogEntity(
      tableName = tableName,
      syncMode = syncMode,
      initiateType = initiateType,
      totalRecords = totalRecords,
      status = SyncStatus.IN_PROGRESS
    )
    val savedLog = syncLogUpdater.createSyncLog(syncLog)

    asyncExecutor.execute {
      syncOrchestrator.process(savedLog)
    }

    return syncLogMapper.toDto(savedLog)
  }
}
