package app.myliberty.easy_records.organizations.business.ledger

import java.math.BigDecimal

interface SubledgerAmountTotals {
    val contactReferenceNumber: String
    val payableTotal: BigDecimal
    val receivableTotal: BigDecimal
}
