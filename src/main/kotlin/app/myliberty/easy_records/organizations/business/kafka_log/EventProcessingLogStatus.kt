package app.myliberty.easy_records.organizations.business.kafka_log

import app.myliberty.easy_records.util.enums.HasCode

enum class EventProcessingLogStatus(override val code: String) : HasCode {
    PENDING("PND"),
    PROCESSED("PRC"),
    FAILED("FAI"),
    RETRYING("RTR"),
    PUBLISH_FAILED("PBF"),
    DLT_PUBLISH_FAILED("DPF")
}
