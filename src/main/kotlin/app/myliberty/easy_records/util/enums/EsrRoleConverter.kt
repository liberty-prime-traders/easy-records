package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class RtsRoleConverter : EnumConverter<RtsRole>(RtsRole::class.java)
