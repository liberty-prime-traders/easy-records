package app.myliberty.easy_records.cross_tier.core.expense.search

import app.myliberty.easy_records.util.enums.PaymentStatus

object ExpenseSearchMapper {

    fun toBucket(voided: Boolean, paymentStatus: PaymentStatus): ExpenseSummaryBucket = when {
        voided -> ExpenseSummaryBucket.VOIDED
        paymentStatus == PaymentStatus.UNPAID -> ExpenseSummaryBucket.UNPAID
        paymentStatus == PaymentStatus.PARTIALLY_SETTLED -> ExpenseSummaryBucket.PARTIAL
        else -> ExpenseSummaryBucket.PAID
    }
}
