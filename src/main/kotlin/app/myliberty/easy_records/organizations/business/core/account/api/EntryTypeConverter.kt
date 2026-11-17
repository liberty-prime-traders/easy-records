package app.myliberty.easy_records.organizations.business.core.account.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class EntryTypeConverter : EnumConverter<EntryType>(EntryType::class.java)
