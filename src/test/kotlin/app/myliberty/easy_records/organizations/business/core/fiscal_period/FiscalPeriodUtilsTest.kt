package app.myliberty.easy_records.organizations.business.core.fiscal_period

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class FiscalPeriodUtilsTest {

    @Test
    fun `yearEnd is the end of the current fiscal year when the date falls on or before its end month`() {
        assertEquals(LocalDate.of(2026, 11, 30), FiscalPeriodUtils.yearEnd(LocalDate.of(2026, 11, 24), 11))
        assertEquals(LocalDate.of(2026, 11, 30), FiscalPeriodUtils.yearEnd(LocalDate.of(2026, 11, 30), 11))
    }

    @Test
    fun `yearEnd moves to the next fiscal year once the date passes the end month`() {
        assertEquals(LocalDate.of(2027, 11, 30), FiscalPeriodUtils.yearEnd(LocalDate.of(2026, 12, 1), 11))
    }

    @Test
    fun `yearStart is the day after the previous fiscal year end`() {
        assertEquals(LocalDate.of(2025, 12, 1), FiscalPeriodUtils.yearStart(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun `fiscalStartMonth follows the month after the year-end month and wraps in December`() {
        assertEquals(12, FiscalPeriodUtils.fiscalStartMonth(11))
        assertEquals(1, FiscalPeriodUtils.fiscalStartMonth(12))
    }

    @Test
    fun `fiscalYearLabel uses one year when the fiscal year lies in one calendar year`() {
        assertEquals("FY2026", FiscalPeriodUtils.fiscalYearLabel(LocalDate.of(2026, 6, 1), 12))
    }

    @Test
    fun `fiscalYearLabel spans two years when the fiscal year crosses a calendar year`() {
        assertEquals("FY2025/26", FiscalPeriodUtils.fiscalYearLabel(LocalDate.of(2026, 11, 24), 11))
    }

    @Test
    fun `daysUntilDayOfWeek is zero for the same weekday and counts forward otherwise`() {
        val monday = LocalDate.of(2026, 11, 23)
        assertEquals(0, FiscalPeriodUtils.daysUntilDayOfWeek(monday, monday.dayOfWeek))
        assertEquals(1, FiscalPeriodUtils.daysUntilDayOfWeek(monday, DayOfWeek.TUESDAY))
        assertEquals(6, FiscalPeriodUtils.daysUntilDayOfWeek(monday, DayOfWeek.SUNDAY))
    }

    @Test
    fun `positionIn445 places days in the first four, five and four week slots of each 13-week group`() {
        val firstPeriodStart = LocalDate.of(2026, 1, 5)
        assertEquals(0, FiscalPeriodUtils.positionIn445(firstPeriodStart, firstPeriodStart))
        assertEquals(0, FiscalPeriodUtils.positionIn445(firstPeriodStart, firstPeriodStart.plusDays(27)))
        assertEquals(1, FiscalPeriodUtils.positionIn445(firstPeriodStart, firstPeriodStart.plusDays(28)))
        assertEquals(2, FiscalPeriodUtils.positionIn445(firstPeriodStart, firstPeriodStart.plusDays(56)))
        assertEquals(0, FiscalPeriodUtils.positionIn445(firstPeriodStart, firstPeriodStart.plusDays(91)))
    }
}
