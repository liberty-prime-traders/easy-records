package app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api

data class AdjustmentReasonInsertDto(
    val name: String,
    val direction: AdjustmentDirection
)
