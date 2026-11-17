package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

enum class ConsolidationProblemKind(val failedAtStep: MergeStep) {
    INACTIVE_PRODUCT(MergeStep.PRODUCT_CHECKS),
    DIFFERENT_LIST_PRICES(MergeStep.PRODUCT_CHECKS),
    UNITS_NOT_EXACT(MergeStep.QUANTITY),
    QUANTITY_TOO_PRECISE(MergeStep.QUANTITY),
    QUANTITY_TOO_LARGE(MergeStep.QUANTITY),
    OVERRIDE_ON_SOME_LINES_ONLY(MergeStep.ADJUSTMENTS),
    TYPED_PRICES_DIFFER(MergeStep.ADJUSTMENTS),
    TYPED_PRICE_NOT_TYPEABLE(MergeStep.ADJUSTMENTS),
    APPROVERS_DIFFER(MergeStep.ADJUSTMENTS),
    OTHER_ADJUSTMENTS_DIFFER(MergeStep.ADJUSTMENTS),
    TOTAL_DRIFT(MergeStep.TOTAL),
}
