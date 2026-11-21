package app.myliberty.easy_records.organizations.business.core.contact.api

import app.myliberty.easy_records.util.enums.HasCode

enum class ContactStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    SUSPENDED("XS"),
    INACTIVE("X")
}
