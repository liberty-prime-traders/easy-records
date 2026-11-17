package app.myliberty.easy_records.organizations.business.core.expense_type

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ExpensePayeeTypeSetConverter : EnumSetConverter<ContactType>(ContactType::class.java)
