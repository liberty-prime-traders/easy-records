package app.myliberty.easy_records.organizations.business.retail.unitgroup.api

import app.myliberty.easy_records.organizations.business.retail.unitgroup.UnitGroupCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitGroupDataFetcher(private val unitGroupCache: UnitGroupCache) {

    fun exists(unitGroupId: UUID?): Boolean =
        unitGroupId != null && unitGroupCache.getAllUnitGroups().any { it.id == unitGroupId }

    fun isMiscellaneous(unitGroupId: UUID?): Boolean =
        unitGroupCache.getAllUnitGroups().any { it.id == unitGroupId && it.code == SystemUnitGroup.MISC.code }
}
