package app.myliberty.easy_records.organizations.business.contact_opening_balance.api

import app.myliberty.easy_records.organizations.business.ledger.api.SubledgerBalanceDto
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.Money
import java.math.BigDecimal
import java.util.Optional

data class ContactOpeningBalanceSetDto(
    val payable: Optional<Money>? = null,
    val receivable: Optional<Money>? = null
) {

    fun applyTo(existing: SubledgerBalanceDto): SubledgerBalanceDto = SubledgerBalanceDto(
        payable = payable?.map { it.toBigDecimal() }?.orElse(Decimals.MoneyScale.round(BigDecimal.ZERO)) ?: existing.payable,
        receivable = receivable?.map { it.toBigDecimal() }?.orElse(Decimals.MoneyScale.round(BigDecimal.ZERO)) ?: existing.receivable
    )
}
