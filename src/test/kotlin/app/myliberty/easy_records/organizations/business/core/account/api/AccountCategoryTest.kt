package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.util.ui_models.BalanceEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountCategoryTest {

    @Test
    fun `a category's increasing effect follows its root account type`() {
        assertEquals(BalanceEffect.ADDS, AccountCategory.ASSET.balanceEffectWhenIncreasing())
        assertEquals(BalanceEffect.SUBTRACTS, AccountCategory.LIABILITY.balanceEffectWhenIncreasing())
    }
}
