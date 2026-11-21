package app.myliberty.easy_records.organizations.business.core.tag.api

import app.myliberty.easy_records.util.enums.HasCode

enum class CategoryType(override val code: String) : HasCode {
    PRODUCT("PRD"),
    EXPENSE("EXP"),
    PURCHASE("PCH")
}
