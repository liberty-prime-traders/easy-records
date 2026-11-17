package app.myliberty.easy_records.organizations.business.account.api

import app.myliberty.easy_records.util.ui_models.BalanceEffect

enum class AccountCategory {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;

    fun balanceEffectWhenIncreasing(): BalanceEffect = AccountType.rootOf(this).balanceEffect()
}
