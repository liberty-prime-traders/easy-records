package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class LoanRepaymentValidator(
    private val loanVoidRepository: LoanVoidRepository,
    private val loanRepaymentVoidRepository: LoanRepaymentVoidRepository
) {

    fun requireRepayable(
        loanDto: LoanDto,
        loanStatusFiguresBeforeRepayment: LoanStatusFigures,
        repaymentDate: LocalDate,
        repaymentAmount: BigDecimal
    ) {
        if (loanVoidRepository.existsByLoanId(loanDto.id)) {
            throw LibertyGenericException("Loan ${loanDto.referenceNumber} is voided")
        }
        if (repaymentDate.isBefore(loanDto.loanDate)) {
            throw LibertyGenericException("Repayment date cannot be before the loan date ${loanDto.loanDate}")
        }
        if (repaymentAmount > loanStatusFiguresBeforeRepayment.outstanding) {
            val repaymentDisplay = DisplayFormatters.formatCurrency(repaymentAmount)
            val outstandingDisplay = DisplayFormatters.formatCurrency(loanStatusFiguresBeforeRepayment.outstanding)
            throw LibertyGenericException(
                "Repayment of $repaymentDisplay exceeds the outstanding $outstandingDisplay " +
                    "on loan ${loanDto.referenceNumber}"
            )
        }
    }

    fun requireVoidable(loanRepaymentDto: LoanRepaymentDto) {
        if (loanRepaymentVoidRepository.existsByLoanRepaymentId(loanRepaymentDto.id)) {
            throw LibertyGenericException("Loan repayment ${loanRepaymentDto.referenceNumber} is already voided")
        }
    }
}
