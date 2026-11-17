package app.myliberty.easy_records.organizations.business.contact_opening_balance.api

import app.myliberty.easy_records.util.enums.HasCode

enum class ContactSubledgerDirection(override val code: String) : HasCode {
    RECEIVABLE("R"),
    PAYABLE("P");

    fun opposite(): ContactSubledgerDirection = when (this) {
        RECEIVABLE -> PAYABLE
        PAYABLE -> RECEIVABLE
    }
}
