package app.myliberty.easy_records.platform.business.sysuser

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.sysuser.api.UserType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class UserTypeConverter : EnumConverter<UserType>(UserType::class.java)
