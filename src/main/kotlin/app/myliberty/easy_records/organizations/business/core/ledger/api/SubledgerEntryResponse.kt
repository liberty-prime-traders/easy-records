package app.myliberty.easy_records.organizations.business.core.ledger.api

import app.myliberty.easy_records.util.business.MoneyDisplay
import app.myliberty.easy_records.util.ui_models.BalanceEffect
import java.time.OffsetDateTime
import java.util.UUID

data class SubledgerEntryResponse(
    val id: UUID,
    val createdOn: OffsetDateTime,
    val groupReferenceNumber: String,
    val sourceType: LedgerSourceType,
    val sourceReferenceNumber: String,
    val transactionAmount: MoneyDisplay,
    val transactionBalanceEffect: BalanceEffect,
    val netBalance: MoneyDisplay,
    val netBalanceEffect: BalanceEffect
)
