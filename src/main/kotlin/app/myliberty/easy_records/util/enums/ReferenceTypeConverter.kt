package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class ReferenceTypeConverter : EnumConverter<ReferenceType>(ReferenceType::class.java)
