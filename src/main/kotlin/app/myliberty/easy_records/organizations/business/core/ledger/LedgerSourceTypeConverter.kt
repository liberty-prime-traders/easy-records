package app.myliberty.easy_records.organizations.business.ledger

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class LedgerSourceTypeConverter : EnumConverter<LedgerSourceType>(LedgerSourceType::class.java)
