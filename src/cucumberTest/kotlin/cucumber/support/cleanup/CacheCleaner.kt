package app.myliberty.easy_records.cucumber.support.cleanup

import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearRequestDto
import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearService
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class CacheCleaner(
    private val cacheClearService: CacheClearService,
    private val cacheManager: CacheManager,
) {

    // Tenant clear evicts only organization-schema keys, so platform-level caches (e.g. JurisdictionType, TaxType)
    // would keep rows the reset truncated. Clear every cache after the truncation.
    fun clearAllCaches() {
        cacheClearService.clearCaches(CacheClearRequestDto())
        cacheManager.cacheNames.forEach { cacheName -> cacheManager.getCache(cacheName)?.clear() }
    }
}
