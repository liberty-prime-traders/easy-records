package app.myliberty.easy_records.cucumber.support.cleanup

import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearRequestDto
import app.myliberty.easy_records.platform.business.cache_admin.api.CacheClearService
import org.springframework.stereotype.Component

@Component
class CacheCleaner(private val cacheClearService: CacheClearService) {

    fun clearAllCaches() {
        cacheClearService.clearCaches(CacheClearRequestDto())
    }
}
