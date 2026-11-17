package app.myliberty.easy_records.organizations.business.fund_movement

import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object FundMovementPostingRequests {

    fun recorded(
        referenceNumber: String,
        debitAccountCode: String,
        creditAccountCode: String,
        amount: BigDecimal,
        movementDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.FUND_MOVEMENT,
            postingDate = movementDate,
            entries = listOf(
                LedgerEntryRequest.debit(debitAccountCode, positiveAmount),
                LedgerEntryRequest.credit(creditAccountCode, positiveAmount)
            ),
            subledgerEntries = emptyList()
        )
    }

    // The movement's own debit and credit accounts, legs swapped; the void shares the movement's reference.
    fun voided(
        referenceNumber: String,
        debitAccountCode: String,
        creditAccountCode: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.FUND_MOVEMENT_VOID,
            postingDate = voidDate,
            entries = listOf(
                LedgerEntryRequest.credit(debitAccountCode, positiveAmount),
                LedgerEntryRequest.debit(creditAccountCode, positiveAmount)
            ),
            subledgerEntries = emptyList(),
            reversal = true
        )
    }
}
