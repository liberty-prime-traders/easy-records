package app.myliberty.easy_records.locations.business.retail.catalog_sync

import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncLogResponseDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface SyncLogMapper {
  fun toDto(syncLogEntity: SyncLogEntity): SyncLogResponseDto
}
