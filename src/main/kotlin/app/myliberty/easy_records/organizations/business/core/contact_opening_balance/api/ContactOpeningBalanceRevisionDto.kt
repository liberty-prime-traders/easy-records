package app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api

import app.myliberty.easy_records.util.ui_models.BalanceEffect
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ContactOpeningBalanceRevisionDto(
    val referenceNumber: String,
    val contactReferenceNumber: String,
    val subledgerDirection: ContactSubledgerDirection,
    val amount: BigDecimal,
    val balanceEffect: BalanceEffect,
    val changedAt: OffsetDateTime,
    val changedBy: String
)
