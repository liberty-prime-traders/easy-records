package app.myliberty.easy_records.organizations.business.core.opening_balance.api

import app.myliberty.easy_records.util.business.Money

data class OpeningBalanceUpsertDto(
    val accountCode: String,
    val newAmount: Money
)
