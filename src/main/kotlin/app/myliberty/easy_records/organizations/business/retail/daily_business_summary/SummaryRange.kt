package app.myliberty.easy_records.organizations.business.retail.daily_business_summary

import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.PeriodStatus
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.time.LocalDate
import java.time.OffsetDateTime

internal class SummaryRange(val from: LocalDate, val before: LocalDate, asOf: OffsetDateTime) {

    val today: LocalDate = DateTimes.Local.atOrganizationZone(asOf)

    val dates: List<LocalDate>

    init {
        if (!from.isBefore(before)) {
            throw LibertyGenericException("'from' must be before 'before'")
        }
        if (before.isAfter(today.plusDays(1))) {
            throw LibertyGenericException("'before' cannot be after tomorrow")
        }
        if (from.plusDays(MAXIMUM_DAYS) < before) {
            throw LibertyGenericException("The range cannot span more than $MAXIMUM_DAYS days")
        }
        dates = generateSequence(from) { date -> date.plusDays(1) }.takeWhile { date -> date.isBefore(before) }.toList()
    }

    val rangeStatus: PeriodStatus = statusOf(dates.last())

    fun statusOf(date: LocalDate): PeriodStatus =
        if (date == today) PeriodStatus.IN_PROGRESS else PeriodStatus.COMPLETE

    companion object {
        const val MAXIMUM_DAYS = 92L
    }
}
