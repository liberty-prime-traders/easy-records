package app.myliberty.easy_records.organizations.business.retail.unitconversion

import app.myliberty.easy_records.configuration.cache.TenantCachesClearedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UnitConversionGraphCacheListener(private val graphBuilder: UnitConversionGraphBuilder) {

    @EventListener
    fun onTenantCachesCleared(@Suppress("UNUSED_PARAMETER") event: TenantCachesClearedEvent) {
        graphBuilder.invalidate()
    }
}
