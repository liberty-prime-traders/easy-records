package app.myliberty.easy_records.util.model

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = false)
class TableNameConverter : EnumConverter<TableName>(TableName::class.java)
