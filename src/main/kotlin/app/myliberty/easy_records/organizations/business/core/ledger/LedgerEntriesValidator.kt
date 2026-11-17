package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.math.BigDecimal

object LedgerEntriesValidator {

    fun validate(entries: List<LedgerEntryRequest>) {
        val totalDebits = entries
            .filter { it.entryType == EntryType.DEBIT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount.toBigDecimal() }
        val totalCredits = entries
            .filter { it.entryType == EntryType.CREDIT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount.toBigDecimal() }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw RtsGenericException("Ledger entries are not balanced — debits: $totalDebits, credits: $totalCredits")
        }
    }
}
