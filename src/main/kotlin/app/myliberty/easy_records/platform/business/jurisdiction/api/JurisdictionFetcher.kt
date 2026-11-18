package app.myliberty.easy_records.platform.business.jurisdiction.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.platform.business.jurisdiction.JurisdictionCache
import org.springframework.stereotype.Service

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class JurisdictionFetcher(private val jurisdictionCache: JurisdictionCache) {
    fun getAllDtos(): Collection<JurisdictionDto> = jurisdictionCache.getAll()
}
