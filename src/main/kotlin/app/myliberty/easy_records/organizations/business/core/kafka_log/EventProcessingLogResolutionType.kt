package app.myliberty.easy_records.organizations.business.core.kafka_log

import app.myliberty.easy_records.util.enums.HasCode

enum class EventProcessingLogResolutionType(override val code: String) : HasCode {
    DLT_REPLAY("RPL"),
    REISSUED("RIS"),
    RACE_LOST("RCL")
}
