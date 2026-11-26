package app.myliberty.easy_records.organizations.business.core.accounting_config.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class OrgAccountingConfigUpdateDtoTest {

    private val existing = OrgAccountingConfigDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        fiscalYearEndMonth = 3,
        fiscalPeriodCycle = FiscalPeriodCycle.MONTHLY,
        periodWeekStartDay = DayOfWeek.MONDAY,
        periodPrepDays = 2
    )

    @Test
    fun `omitted fields keep the existing values`() {
        val updated = OrgAccountingConfigUpdateDto().applyTo(existing)

        assertEquals(existing, updated)
    }

    @Test
    fun `provided fields replace the existing values`() {
        val updateDto = OrgAccountingConfigUpdateDto(
            fiscalYearEndMonth = Optional.of(6),
            fiscalPeriodCycle = Optional.of(FiscalPeriodCycle.QUARTERLY),
            periodWeekStartDay = Optional.of(DayOfWeek.SUNDAY),
            periodPrepDays = Optional.of(4)
        )

        val updated = updateDto.applyTo(existing)

        assertEquals(6, updated.fiscalYearEndMonth)
        assertEquals(FiscalPeriodCycle.QUARTERLY, updated.fiscalPeriodCycle)
        assertEquals(DayOfWeek.SUNDAY, updated.periodWeekStartDay)
        assertEquals(4, updated.periodPrepDays)
    }

    @Test
    fun `an explicitly empty optional keeps the existing value because the field is not nullable`() {
        val updated = OrgAccountingConfigUpdateDto(periodPrepDays = Optional.empty()).applyTo(existing)

        assertEquals(2, updated.periodPrepDays)
    }
}
