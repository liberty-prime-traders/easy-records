package app.myliberty.easy_records.organizations.business.retail.unitgroup

import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupDto
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.util.UUID

object UnitGroupNameRules {

    const val LOCK_KEY = "names"

    fun requireAvailable(existingUnitGroupDtos: Collection<UnitGroupDto>, names: List<String>, excludedUnitGroupId: UUID? = null) {
        names.forEachIndexed { index, name ->
            val takenByEarlierName = names.take(index).any { StringUtils.isEquivalentOnAlphanumerics(it, name) }
            val takenByExistingGroup = existingUnitGroupDtos.any {
                it.id != excludedUnitGroupId && StringUtils.isEquivalentOnAlphanumerics(it.name, name)
            }
            if (takenByEarlierName || takenByExistingGroup) {
                throw RtsGenericException("An UnitGroup using the name '$name' already exists")
            }
        }
    }
}
