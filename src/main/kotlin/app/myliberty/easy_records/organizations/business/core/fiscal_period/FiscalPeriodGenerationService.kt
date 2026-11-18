package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.strategy.PeriodCycleStrategyRegistry
import app.myliberty.easy_records.organizations.business.core.fiscal_period.strategy.PeriodRange
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@TransactionalOnOrganizationSchema
class FiscalPeriodGenerationService(
    private val configService: OrgAccountingConfigService,
    private val repository: FiscalPeriodRepository,
    private val strategyRegistry: PeriodCycleStrategyRegistry
) {

    fun generatePeriods() {
        val config = configService.getConfig() ?: return
        val today = DateTimes.Local.Now.organization()
        var lastPeriodEnd = repository.findTopByOrderByEndDateDesc()?.endDate ?: today.minusDays(1)
        val strategy = strategyRegistry.get(config.fiscalPeriodCycle)

        while (ChronoUnit.DAYS.between(today, lastPeriodEnd) <= config.periodPrepDays) {
            val nextStart = lastPeriodEnd.plusDays(1)
            val cleanStart = strategy.nextCleanStart(nextStart, config)
            lastPeriodEnd = if (cleanStart.isAfter(nextStart)) {
                savePeriod(nextStart, PeriodRange(nextStart, cleanStart.minusDays(1)), config, stub = true)
            } else {
                savePeriod(nextStart, strategy.nextPeriod(lastPeriodEnd, config), config, stub = false)
            }
        }
    }

    private fun savePeriod(
        naturalStart: LocalDate,
        range: PeriodRange,
        config: OrgAccountingConfigDto,
        stub: Boolean
    ): LocalDate {
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(naturalStart, config.fiscalYearEndMonth)
        val isYearEnd = !range.end.isBefore(fiscalYearEnd)
        val clampedEnd = if (isYearEnd) fiscalYearEnd else range.end
        val name = if (stub) FiscalPeriodNameGenerator.stubName(naturalStart)
                   else FiscalPeriodNameGenerator.generate(naturalStart, config)
        repository.save(FiscalPeriodEntity(
            name = name,
            startDate = naturalStart,
            endDate = clampedEnd,
            yearEnd = isYearEnd,
            stub = stub
        ))
        return clampedEnd
    }
}
