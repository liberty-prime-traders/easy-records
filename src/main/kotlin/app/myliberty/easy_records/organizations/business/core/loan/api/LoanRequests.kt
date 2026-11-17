package app.myliberty.easy_records.organizations.business.loan.api

import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.LocalDate
import java.util.UUID

data class LoanCreateRequest(
    val lenderContactReference: String,
    val assetAccountCode: String,
    val amount: PositiveMoney,
    val loanDate: LocalDate,
    val note: String? = null
)

data class LoanRepaymentCreateRequest(
    val loanId: UUID,
    val assetAccountCode: String,
    val amount: PositiveMoney,
    val repaymentDate: LocalDate,
    val note: String? = null
)

data class LoanVoidRequest(
    val loanId: UUID,
    val reason: String? = null
)

data class LoanRepaymentVoidRequest(
    val repaymentId: UUID,
    val reason: String? = null
)
