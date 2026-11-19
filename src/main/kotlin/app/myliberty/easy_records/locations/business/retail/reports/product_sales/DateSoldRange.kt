package app.myliberty.easy_records.locations.business.retail.reports.product_sales

import java.time.OffsetDateTime

data class DateSoldRange(val fromInclusive: OffsetDateTime, val beforeExclusive: OffsetDateTime)
