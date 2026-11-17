package app.myliberty.easy_records.cross_tier.expense

import app.myliberty.easy_records.util.enums.HasCode

enum class ExpenseSourceType(override val code: String) : HasCode {
    PURCHASE("PUR"),
    SALE("SAL"),
    STOCK_TRANSFER("TRF"),
    WAGES("WAG"),
    ADHOC("ADH");

    fun batchDescriptionLabel(): String = when (this) {
        PURCHASE -> "Purchase"
        SALE -> "Sale"
        STOCK_TRANSFER -> "Stock transfer"
        WAGES -> "Wages"
        ADHOC -> "Expenses"
    }
}
