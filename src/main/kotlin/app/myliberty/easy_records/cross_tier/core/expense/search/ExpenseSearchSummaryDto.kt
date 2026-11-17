package app.myliberty.easy_records.cross_tier.core.expense.search

import java.math.BigDecimal
import java.util.UUID

enum class ExpenseSummaryBucket {
    UNPAID,
    PARTIAL,
    PAID,
    VOIDED
}

data class ExpenseBucketSummaryDto(
    val bucket: ExpenseSummaryBucket,
    val expenseCount: Long,
    val amountTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val outstandingTotal: BigDecimal
)

data class ExpenseTypeSummaryDto(
    val expenseTypeId: UUID,
    val expenseTypeName: String,
    val expenseCount: Long,
    val amountTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val outstandingTotal: BigDecimal,
    val voidedCount: Long,
    val voidedAmountTotal: BigDecimal
)

data class ExpenseSearchSummaryResponseDto(
    val byStatus: List<ExpenseBucketSummaryDto>,
    val byExpenseType: List<ExpenseTypeSummaryDto>,
    val expenseCount: Long,
    val amountTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val outstandingTotal: BigDecimal,
    val voidedCount: Long,
    val voidedAmountTotal: BigDecimal
)
