package app.myliberty.easy_records.organizations.business.adjustment_reason.api

data class AdjustmentReasonInsertDto(
    val name: String,
    val direction: AdjustmentDirection
)
