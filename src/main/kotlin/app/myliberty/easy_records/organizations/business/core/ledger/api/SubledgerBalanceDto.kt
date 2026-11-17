package app.myliberty.easy_records.organizations.business.core.ledger.api

import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.ui_models.BalanceEffect
import java.math.BigDecimal

data class SubledgerBalanceDto(
    val payable: BigDecimal,
    val receivable: BigDecimal
) {
    val netBalance: BigDecimal = receivable.subtract(payable).abs()

    // Read from the org's POV: the contact owing us is money the org stands to receive.
    val balanceEffect: BalanceEffect = BalanceEffect.ofSignedAmount(receivable.subtract(payable))

    companion object {
        val ZERO = SubledgerBalanceDto(
            payable = Decimals.Scale4.round(BigDecimal.ZERO),
            receivable = Decimals.Scale4.round(BigDecimal.ZERO)
        )
    }
}
