package app.myliberty.easy_records.platform.business.tax_type.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.tax_type.TaxTypeCache
import org.springframework.stereotype.Service

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class TaxTypeFetcher(private val taxTypeCache: TaxTypeCache) {
    fun getAllDtos(): Collection<TaxTypeDto> = taxTypeCache.getAll()
}
