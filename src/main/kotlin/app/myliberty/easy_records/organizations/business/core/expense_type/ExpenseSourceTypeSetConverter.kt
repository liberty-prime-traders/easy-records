package app.myliberty.easy_records.organizations.business.core.expense_type

import jakarta.persistence.Converter
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ExpenseSourceTypeSetConverter : EnumSetConverter<ExpenseSourceType>(ExpenseSourceType::class.java)
