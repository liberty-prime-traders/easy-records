package app.myliberty.easy_records.organizations.business.core.accounting_config.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.accounting_config.OrgAccountingConfigCache
import app.myliberty.easy_records.organizations.business.core.accounting_config.OrgAccountingConfigMapper
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.DayOfWeek

@Service
@TransactionalOnOrganizationSchema
class OrgAccountingConfigService(
    private val configCache: OrgAccountingConfigCache,
    private val mapper: OrgAccountingConfigMapper
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun get(): OrgAccountingConfigResponseDto {
        val config = configCache.get() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        return mapper.toResponseDto(config)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getConfig(): OrgAccountingConfigDto? = configCache.get()

    fun initialize() {
        if (configCache.get() != null) return
        val today = DateTimes.Local.Now.organization()
        configCache.create(OrgAccountingConfigInsertDto(
            fiscalYearEndMonth = today.monthValue,
            fiscalPeriodCycle = FiscalPeriodCycle.MONTHLY,
            periodWeekStartDay = DayOfWeek.MONDAY,
            periodPrepDays = 2
        ))
    }

    fun update(dto: OrgAccountingConfigUpdateDto): OrgAccountingConfigResponseDto {
        val existing = configCache.get() ?: throw RtsGenericException("Accounting configuration has not been initialized")
        val updated = configCache.save(dto.applyTo(existing))
        return mapper.toResponseDto(updated)
    }
}
