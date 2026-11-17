package app.myliberty.easy_records.platform.business.identity

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PendingLinkStatusConverter : EnumConverter<PendingLinkStatus>(PendingLinkStatus::class.java)
