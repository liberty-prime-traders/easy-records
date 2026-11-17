package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class RtsPermissionConverter : EnumConverter<RtsPermission>(RtsPermission::class.java)
