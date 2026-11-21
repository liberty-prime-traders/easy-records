package app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api

import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.LocalDate
import java.util.UUID

data class ContactOpeningSettlementCreateRequest(
    val contactId: UUID,
    val subledgerDirection: ContactSubledgerDirection,
    val paymentMethodId: UUID,
    val amount: PositiveMoney,
    val settlementDate: LocalDate,
    val note: String? = null
)

data class ContactOpeningSettlementVoidRequest(
    val settlementId: UUID,
    val reason: String? = null
)
