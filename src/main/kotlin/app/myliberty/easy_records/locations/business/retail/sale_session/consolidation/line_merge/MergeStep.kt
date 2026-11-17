package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

enum class MergeStep(val order: Int) {
    PRODUCT_CHECKS(1),
    QUANTITY(2),
    ADJUSTMENTS(3),
    TOTAL(4),
}
