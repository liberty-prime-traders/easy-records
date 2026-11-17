package app.myliberty.easy_records.organizations.business.core.fiscal_period.strategy

import app.myliberty.easy_records.organizations.business.core.accounting_config.api.FiscalPeriodCycle
import org.springframework.stereotype.Component

@Component
class PeriodCycleStrategyRegistry {

    private val strategies: Map<FiscalPeriodCycle, PeriodCycleStrategy> = mapOf(
        FiscalPeriodCycle.WEEKLY to WeeklyPeriodStrategy(),
        FiscalPeriodCycle.MONTHLY to MonthlyPeriodStrategy(),
        FiscalPeriodCycle.QUARTERLY to QuarterlyPeriodStrategy(),
        FiscalPeriodCycle.SEMI_ANNUAL to SemiAnnualPeriodStrategy(),
        FiscalPeriodCycle.ANNUAL to AnnualPeriodStrategy(),
        FiscalPeriodCycle.FOUR_FOUR_FIVE to FourFourFivePeriodStrategy()
    )

    fun get(cycle: FiscalPeriodCycle): PeriodCycleStrategy = strategies.getValue(cycle)
}
