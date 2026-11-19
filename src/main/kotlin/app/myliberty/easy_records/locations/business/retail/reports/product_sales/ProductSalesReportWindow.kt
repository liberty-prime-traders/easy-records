package app.myliberty.easy_records.locations.business.retail.reports.product_sales

import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.time.LocalDate

// Whole organization-zone days, so the day buckets combine into any range. before is the day after the last day.
data class ProductSalesReportWindow(val from: LocalDate, val before: LocalDate) {

  val days: List<LocalDate> = from.datesUntil(before).toList()

  fun toDateRange(): DateSoldRange = DateSoldRange(
    DateTimes.Offset.atStartOfDayInOrganizationZone(from),
    DateTimes.Offset.atStartOfDayInOrganizationZone(before)
  )

  companion object {

    fun ofInclusiveDates(from: LocalDate?, to: LocalDate?): ProductSalesReportWindow {
      if (from == null || to == null) {
        throw LibertyGenericException("from and to are required")
      }
      if (from.isAfter(to)) {
        throw LibertyGenericException("from must not be after to")
      }
      if (!to.isBefore(from.plusYears(1))) {
        throw LibertyGenericException("from and to must be less than one year apart")
      }
      return ProductSalesReportWindow(from, to.plusDays(1))
    }
  }
}
