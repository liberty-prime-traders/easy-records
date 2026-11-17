package app.myliberty.easy_records.organizations.business.core.ledger

import java.math.BigDecimal

interface SubledgerAmountTotals {
    val contactReferenceNumber: String
    val payableTotal: BigDecimal
    val receivableTotal: BigDecimal
}
