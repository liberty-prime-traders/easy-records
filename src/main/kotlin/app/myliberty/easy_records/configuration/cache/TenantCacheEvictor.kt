package app.myliberty.easy_records.configuration.cache

import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Component

@Component
class TenantCacheEvictor(private val cacheManager: CacheManager) {

    // Keys are built by TenantCacheConfig as "<schema>:<class>:<method>:<params>"
    // so the trailing colon keeps "org-a" from matching "org-ab".
    fun evictSchemas(schemaNames: Collection<String>): Int {
        val keyPrefixes = schemaNames.map { "$it:" }
        var evictedEntries = 0
        cacheManager.cacheNames.mapNotNull { cacheManager.getCache(it) as? CaffeineCache }.forEach { cache ->
            val matchingKeys = cache.nativeCache.asMap().keys.filter { key ->
                key is String && keyPrefixes.any { key.startsWith(it) }
            }
            matchingKeys.forEach { cache.evict(it) }
            evictedEntries += matchingKeys.size
        }
        return evictedEntries
    }
}
