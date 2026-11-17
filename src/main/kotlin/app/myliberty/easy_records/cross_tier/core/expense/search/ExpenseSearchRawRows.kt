package app.myliberty.easy_records.cross_tier.expense.search

import app.myliberty.easy_records.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseSearchRawRow(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime
)

data class ExpenseSummaryRawRow(
    val expenseTypeId: UUID,
    val voided: Boolean,
    val paymentStatus: PaymentStatus,
    val expenseCount: Long,
    val amountTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val outstandingTotal: BigDecimal
)
