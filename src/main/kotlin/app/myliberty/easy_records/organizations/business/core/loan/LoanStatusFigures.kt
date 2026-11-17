package app.myliberty.easy_records.organizations.business.loan

import app.myliberty.easy_records.organizations.business.loan.api.LoanStatus
import java.math.BigDecimal

data class LoanStatusFigures(
    val amountRepaid: BigDecimal,
    val outstanding: BigDecimal,
    val status: LoanStatus
)
