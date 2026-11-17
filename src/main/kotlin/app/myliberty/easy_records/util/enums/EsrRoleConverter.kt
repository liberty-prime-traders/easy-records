package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class EsrRoleConverter : EnumConverter<EsrRole>(EsrRole::class.java)
