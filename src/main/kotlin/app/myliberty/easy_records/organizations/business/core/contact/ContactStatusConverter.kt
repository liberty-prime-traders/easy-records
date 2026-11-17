package app.myliberty.easy_records.organizations.business.contact

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.contact.api.ContactStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class ContactStatusConverter : EnumConverter<ContactStatus>(ContactStatus::class.java)
