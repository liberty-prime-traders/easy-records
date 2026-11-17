package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class ContactSubledgerDirectionConverter : EnumConverter<ContactSubledgerDirection>(ContactSubledgerDirection::class.java)
