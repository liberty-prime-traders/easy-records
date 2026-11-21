package app.myliberty.easy_records.organizations.business.core.kafka_log

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class EventProcessingLogStatusConverter : EnumConverter<EventProcessingLogStatus>(EventProcessingLogStatus::class.java)
