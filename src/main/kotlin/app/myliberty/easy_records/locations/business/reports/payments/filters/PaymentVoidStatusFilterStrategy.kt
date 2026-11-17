package app.myliberty.easy_records.locations.business.reports.payments.filters

import app.myliberty.easy_records.locations.business.reports.payments.api.PaymentVoidStatusFilter
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext

class PaymentVoidStatusFilterStrategy(
  private val statuses: Set<PaymentVoidStatusFilter>,
  private val voidIdColumn: String
) : FilterStrategy {

  // Both statuses (or none) keep every row, so only a single status adds a clause.
  override fun apply(context: QueryBuilderContext) {
    val onlyStatus = statuses.singleOrNull() ?: return
    context.whereClauses.add(
      when (onlyStatus) {
        PaymentVoidStatusFilter.ACTIVE -> "$voidIdColumn IS NULL"
        PaymentVoidStatusFilter.VOIDED -> "$voidIdColumn IS NOT NULL"
      }
    )
  }
}
