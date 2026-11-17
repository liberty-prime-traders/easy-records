package app.myliberty.easy_records.organizations.business.tag

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.tag.api.CategoryType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class CategoryTypeConverter : EnumConverter<CategoryType>(CategoryType::class.java)
