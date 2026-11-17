package app.myliberty.easy_records.locations.business.catalog_sync.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.catalog_sync.SyncLogEntity
import app.myliberty.easy_records.locations.business.catalog_sync.SyncLogMapper
import app.myliberty.easy_records.locations.business.catalog_sync.SyncLogRepository
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SyncLogFetcher(
    private val syncLogRepository: SyncLogRepository,
    private val syncLogMapper: SyncLogMapper
)  {

    fun findById(syncLogId: UUID): SyncLogEntity {
        return syncLogRepository.findById(syncLogId).orElseThrow {
            IllegalArgumentException("SyncLog not found: $syncLogId")
        }
    }

    fun findInProgressSync(tableName: TableName): SyncLogEntity? {
        return syncLogRepository.findInProgressSync(tableName)
    }

    fun existsInProgressSync(tableName: TableName): Boolean {
        return syncLogRepository.existsInProgressSync(tableName)
    }

    fun getSyncLogById(syncLogId: UUID): SyncLogResponseDto? {
        return try {
            syncLogMapper.toDto(findById(syncLogId))
        } catch(_: IllegalArgumentException) {
            null
        }

    }

    fun findResumableRevision(tableName: TableName): Long? {
        val failedRevision = syncLogRepository.findLastFailedSyncRevision(tableName)
        if (failedRevision != null) {
            return failedRevision
        }
        val cancelledRevision = syncLogRepository.findLastCancelledSyncRevision(tableName)
        if (cancelledRevision != null) {
            return cancelledRevision
        }
        return syncLogRepository.findLastSuccessfulSyncRevision(tableName)
    }

    fun isSyncCancelled(syncLogId: UUID): Boolean {
        return syncLogRepository.isCancelled(syncLogId)
    }

    fun findTopN(limit: Int): List<SyncLogResponseDto> {
        return syncLogRepository.findTopN(limit).map { syncLogMapper.toDto(it) }
    }
}
