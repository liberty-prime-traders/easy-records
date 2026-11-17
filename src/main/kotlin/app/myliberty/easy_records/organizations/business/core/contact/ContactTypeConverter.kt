package app.myliberty.easy_records.organizations.business.core.contact

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ContactTypeConverter : EnumSetConverter<ContactType>(ContactType::class.java)
