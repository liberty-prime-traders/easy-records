package app.myliberty.easy_records.locations.business.product_sales_report

import java.time.OffsetDateTime

data class DateSoldRange(val fromInclusive: OffsetDateTime, val beforeExclusive: OffsetDateTime)
