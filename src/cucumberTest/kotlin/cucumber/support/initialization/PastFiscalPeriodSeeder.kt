package app.myliberty.easy_records.cucumber.support.initialization

import app.myliberty.easy_records.cucumber.support.database.SchemaScopedRunner
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodEntity
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodNameGenerator
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodRepository
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodUtils
import org.springframework.stereotype.Component

// Generation only ever looks forward from today, so a past date has no open period unless one is seeded here.
@Component
class PastFiscalPeriodSeeder(
  private val fiscalPeriodRepository: FiscalPeriodRepository,
  private val orgAccountingConfigService: OrgAccountingConfigService,
  private val schemaScopedRunner: SchemaScopedRunner
) {

  fun seed() {
    schemaScopedRunner.run(FiscalPeriodEntity::class.java) {
      val currentPeriod = checkNotNull(fiscalPeriodRepository.findTopByOrderByEndDateDesc()) {
        "Generate the current fiscal period before seeding a past one"
      }
      if (fiscalPeriodRepository.findAll().any { it.endDate < currentPeriod.startDate }) return@run
      val fiscalYearEndMonth = checkNotNull(orgAccountingConfigService.getConfig()) {
        "Accounting configuration has not been initialized"
      }.fiscalYearEndMonth
      val pastPeriodEnd = currentPeriod.startDate.minusDays(1)
      val fiscalYearEnd = FiscalPeriodUtils.yearEnd(pastPeriodEnd, fiscalYearEndMonth)
      // A period never spans a fiscal year end, so a window that would is cut to start after that year end.
      val earliestStart = FiscalPeriodUtils.yearStart(fiscalYearEnd)
      val pastPeriodStart = maxOf(pastPeriodEnd.minusDays(PAST_PERIOD_DAYS - 1), earliestStart)
      fiscalPeriodRepository.save(
        FiscalPeriodEntity(
          name = FiscalPeriodNameGenerator.stubName(pastPeriodStart),
          startDate = pastPeriodStart,
          endDate = pastPeriodEnd,
          yearEnd = !pastPeriodEnd.isBefore(fiscalYearEnd),
          stub = true
        ).also { it.createdById = currentPeriod.createdById }
      )
    }
  }

  companion object {
    private const val PAST_PERIOD_DAYS = 30L
  }
}
