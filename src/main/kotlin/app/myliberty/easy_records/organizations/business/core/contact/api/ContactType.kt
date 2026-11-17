package app.myliberty.easy_records.organizations.business.contact.api

import app.myliberty.easy_records.util.enums.HasCode

enum class ContactType(override val code: String) : HasCode {
    CUSTOMER("CUS"),
    SERVICE_PROVIDER("SPR"),
    SUPPLIER("SUP"),
    EMPLOYEE("EMP"),
    GOVERNMENT("GOV"),
    LENDER("LEND"),
    OTHER("OTH")
}
