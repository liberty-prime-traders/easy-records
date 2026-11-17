package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class SchemaOwnerTypeConverter : EnumConverter<SchemaOwnerType>(SchemaOwnerType::class.java)
