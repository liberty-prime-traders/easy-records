package app.myliberty.easy_records.configuration.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.cache.caffeine.CaffeineCacheManager

class TenantCacheEvictorTest {

    private val cacheManager = CaffeineCacheManager("Product", "Location")
    private val tenantCacheEvictor = TenantCacheEvictor(cacheManager)

    private fun put(cacheName: String, key: String) = cacheManager.getCache(cacheName)!!.put(key, "value")

    private fun cached(cacheName: String, key: String) = cacheManager.getCache(cacheName)!!.get(key)

    @Test
    fun `evicts the entries of the given schemas from every cache and keeps the others`() {
        put("Product", "org-a:OrganizationProductCache:findAllProducts:[]")
        put("Location", "org-a:LocationCache:getAllLocations:[]")
        put("Product", "loc-1:LocationProductCache:findAll:[]")
        put("Product", "org-b:OrganizationProductCache:findAllProducts:[]")

        val evictedEntries = tenantCacheEvictor.evictSchemas(listOf("org-a", "loc-1"))

        assertEquals(3, evictedEntries)
        assertNull(cached("Product", "org-a:OrganizationProductCache:findAllProducts:[]"))
        assertNull(cached("Location", "org-a:LocationCache:getAllLocations:[]"))
        assertNull(cached("Product", "loc-1:LocationProductCache:findAll:[]"))
        assertNotNull(cached("Product", "org-b:OrganizationProductCache:findAllProducts:[]"))
    }

    @Test
    fun `a schema name does not match another schema that starts with it`() {
        put("Product", "org-ab:OrganizationProductCache:findAllProducts:[]")

        val evictedEntries = tenantCacheEvictor.evictSchemas(listOf("org-a"))

        assertEquals(0, evictedEntries)
        assertNotNull(cached("Product", "org-ab:OrganizationProductCache:findAllProducts:[]"))
    }

    @Test
    fun `no schemas evicts nothing`() {
        put("Product", "org-a:OrganizationProductCache:findAllProducts:[]")

        assertEquals(0, tenantCacheEvictor.evictSchemas(emptyList()))
        assertNotNull(cached("Product", "org-a:OrganizationProductCache:findAllProducts:[]"))
    }
}
