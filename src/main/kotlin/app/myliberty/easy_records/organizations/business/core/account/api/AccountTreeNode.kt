package app.myliberty.easy_records.organizations.business.account.api

import java.math.BigDecimal

data class AccountTreeNode(
    val code: String,
    val name: String,
    val parentAccountCode: String?,
    val category: AccountCategory,
    val isContra: Boolean,
    val normalBalance: EntryType,
    val isLeaf: Boolean
) {
    fun amountInNormalDirection(entryType: EntryType, amount: BigDecimal): BigDecimal =
        entryType.amountTowards(normalBalance, amount)
}
