package app.myliberty.easy_records.cross_tier.core.expense.entities

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter
class ExpenseSourceTypeConverter : EnumConverter<ExpenseSourceType>(ExpenseSourceType::class.java)
