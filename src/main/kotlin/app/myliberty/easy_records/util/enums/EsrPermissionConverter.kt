package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class EsrPermissionConverter : EnumConverter<EsrPermission>(EsrPermission::class.java)
