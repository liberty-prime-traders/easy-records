package app.myliberty.easy_records.organizations.business.account.api

import app.myliberty.easy_records.util.ui_models.BalanceEffect
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class AccountResponseDto(
    val id: UUID,
    val code: String,
    val name: String,
    val displayName: String,
    val accountType: AccountType,
    val accountIsActive: Boolean,
    val accountIsSystemMaintained: Boolean,
    val currentBalance: BigDecimal,
    val parentAccountCode: String?,
    val parentAccount: String?,
    val accountIsExtensible: Boolean,
    val balanceEffect: BalanceEffect,
    val openingBalance: BigDecimal
) : Serializable
