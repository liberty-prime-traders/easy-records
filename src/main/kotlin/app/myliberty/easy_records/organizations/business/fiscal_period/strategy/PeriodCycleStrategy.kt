package app.myliberty.easy_records.organizations.business.fiscal_period.strategy

import app.myliberty.easy_records.organizations.business.accounting_config.api.OrgAccountingConfigDto
import java.time.LocalDate

interface PeriodCycleStrategy {

    fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange

    fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate
}
