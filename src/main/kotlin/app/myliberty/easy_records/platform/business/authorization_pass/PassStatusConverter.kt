package app.myliberty.easy_records.platform.business.authorization_pass

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.authorization_pass.api.PassStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PassStatusConverter : EnumConverter<PassStatus>(PassStatus::class.java)
