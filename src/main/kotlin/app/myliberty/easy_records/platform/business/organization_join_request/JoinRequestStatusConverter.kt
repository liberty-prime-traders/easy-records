package app.myliberty.easy_records.platform.business.organization_join_request

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.organization_join_request.api.JoinRequestStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class JoinRequestStatusConverter : EnumConverter<JoinRequestStatus>(JoinRequestStatus::class.java)
