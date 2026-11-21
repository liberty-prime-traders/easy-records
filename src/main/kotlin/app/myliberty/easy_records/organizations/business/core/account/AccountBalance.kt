package app.myliberty.easy_records.organizations.business.core.account

import java.math.BigDecimal

interface AccountBalance {
    val code: String
    val currentBalance: BigDecimal
}
