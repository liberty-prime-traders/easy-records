package app.myliberty.easy_records.locations.business.stock_revision.api

import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import app.myliberty.easy_records.organizations.business.account.api.EntryType
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.PostedStockAdjustmentAccounts
import java.math.BigDecimal

data class AdjustmentCostChange(
    val debitAccountCode: String,
    val creditAccountCode: String,
    val costChange: BigDecimal
) {
    // The adjustment debits one account and credits another by the cost change; only revenue and expense accounts move
    // profit.
    fun netProfitChange(): BigDecimal =
        accountTypeOf(debitAccountCode).profitChangeOf(EntryType.DEBIT, costChange)
            .add(accountTypeOf(creditAccountCode).profitChangeOf(EntryType.CREDIT, costChange))

    private fun accountTypeOf(accountCode: String) = SystemAccount.entries.first { it.code == accountCode }.type
}

object RevisionLedgerEntries {

    fun purchaseDeliveryRevision(
        revisedLineTotal: BigDecimal,
        replacementLineTotal: BigDecimal
    ): List<LedgerEntryRequest> {
        val changeInLineTotal = replacementLineTotal.subtract(revisedLineTotal)
        return if (changeInLineTotal.signum() > 0) {
            debitAndCredit(SystemAccount.INVENTORY, SystemAccount.TRADE_PAYABLES, changeInLineTotal)
        } else {
            debitAndCredit(SystemAccount.TRADE_PAYABLES, SystemAccount.INVENTORY, changeInLineTotal.abs())
        }
    }

    fun saleCogsRestatement(oldCostTotal: BigDecimal, newCostTotal: BigDecimal): List<LedgerEntryRequest> {
        val costChange = newCostTotal.subtract(oldCostTotal)
        return if (costChange.signum() > 0) {
            debitAndCredit(SystemAccount.COST_OF_GOODS_SOLD, SystemAccount.INVENTORY, costChange)
        } else {
            debitAndCredit(SystemAccount.INVENTORY, SystemAccount.COST_OF_GOODS_SOLD, costChange.abs())
        }
    }

    fun stockAdjustmentRestatement(adjustmentCostChanges: List<AdjustmentCostChange>): List<LedgerEntryRequest> =
        adjustmentCostChanges
            .flatMap { listOf(it.debitAccountCode to it.costChange, it.creditAccountCode to it.costChange.negate()) }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .flatMap { (accountCode, legs) ->
                val debitMinusCredit = legs.fold(BigDecimal.ZERO, BigDecimal::add)
                if (debitMinusCredit.signum() > 0) {
                    LedgerEntryRequest.debitUnlessZero(accountCode, debitMinusCredit)
                } else {
                    LedgerEntryRequest.creditUnlessZero(accountCode, debitMinusCredit.negate())
                }
            }

    // The accounts are the ones the original posting used, so a later change of the category rules never splits an
    // adjustment across two sets of accounts. The rule accounts apply when there are none to use.
    fun adjustmentCostChangeOf(
        restatement: StockAdjustmentCostRestatementDto,
        postedAccounts: PostedStockAdjustmentAccounts?
    ) = AdjustmentCostChange(
        debitAccountCode = postedAccounts?.debitAccountCode ?: restatement.currentRuleDebitAccountCode,
        creditAccountCode = postedAccounts?.creditAccountCode ?: restatement.currentRuleCreditAccountCode,
        costChange = restatement.newCostTotal.toBigDecimal().subtract(restatement.oldCostTotal.toBigDecimal())
    )

    private fun debitAndCredit(
        debit: SystemAccount,
        credit: SystemAccount,
        amount: BigDecimal
    ): List<LedgerEntryRequest> =
        buildList {
            addAll(LedgerEntryRequest.debitUnlessZero(debit.code, amount))
            addAll(LedgerEntryRequest.creditUnlessZero(credit.code, amount))
        }
}
