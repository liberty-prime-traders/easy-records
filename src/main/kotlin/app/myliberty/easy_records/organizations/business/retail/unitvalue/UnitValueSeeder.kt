package app.myliberty.easy_records.organizations.business.retail.unitvalue

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.org_profile.api.OrgDataSeeder
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupService
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.SystemUnitValue
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(OrgDataSeeder.UNIT_VALUE)
@TransactionalOnOrganizationSchema
class UnitValueSeeder(
    private val unitValueCache: UnitValueCache,
    private val unitGroupService: UnitGroupService
) : OrgDataSeeder {

    override fun seed() {
        val unitGroups = unitGroupService.getAllUnitGroupDtos().associateBy { it.code }
        val existingValues = unitValueCache.getAllUnitValues().associateBy { it.code }.toMutableMap()
        val existingCodes = existingValues.keys

        SystemUnitValue.entries
            .filter { it.code !in existingCodes }
            .forEach { unitValue ->
                val groupId = unitGroups[unitValue.group.code]?.id ?: return@forEach
                val baseUnitId = unitValue.baseUnit?.let { existingValues[it.code]?.id }
                val entity = UnitValueEntity(
                    name = unitValue.unitName,
                    code = unitValue.code,
                    unitGroupId = groupId,
                    baseUnit = baseUnitId,
                    unitsOfBasePerUnit = unitValue.unitsOfBasePerUnit,
                    systemDefined = true
                )
                existingValues[unitValue.code] = unitValueCache.save(entity)
            }
    }
}
