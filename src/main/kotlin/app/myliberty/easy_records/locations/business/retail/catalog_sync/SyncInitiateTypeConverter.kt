package app.myliberty.easy_records.locations.business.retail.catalog_sync

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncInitiateType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class SyncInitiateTypeConverter : EnumConverter<SyncInitiateType>(SyncInitiateType::class.java)
