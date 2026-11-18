package app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api

import java.time.LocalDate

data class DailyBusinessSummaryRequest(
    val from: LocalDate,
    val before: LocalDate
)
