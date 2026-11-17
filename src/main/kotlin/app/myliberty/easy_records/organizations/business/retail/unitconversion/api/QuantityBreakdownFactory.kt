package app.myliberty.easy_records.organizations.business.retail.unitconversion.api

import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QuantityBreakdownFactory(
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    fun forBaseUnits(baseUnitIds: Collection<UUID>): Map<UUID, QuantityBreakdown> {
        if (baseUnitIds.isEmpty()) return emptyMap()
        val breakdownUnits = unitConversionGraphFacade.getOrLoad().getBreakdownUnits()
        return baseUnitIds.toSet().associateWith { QuantityBreakdown.forBaseUnit(breakdownUnits, it) }
    }
}
