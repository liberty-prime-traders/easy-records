package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class SchemaLevelConverter: EnumConverter<SchemaLevel>(SchemaLevel::class.java)
