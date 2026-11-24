package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class LoanVoidValidator(
    private val loanVoidRepository: LoanVoidRepository,
    private val activeLoanRepaymentRepository: ActiveLoanRepaymentRepository
) {

    fun requireVoidable(loanDto: LoanDto) {
        if (loanVoidRepository.existsByLoanId(loanDto.id)) {
            throw LibertyGenericException("Loan ${loanDto.referenceNumber} is already voided")
        }
        val activeRepayments = activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanDto.id)
        val activeRepaymentReferences = activeRepayments.map { activeRepayment -> activeRepayment.referenceNumber }
        if (activeRepaymentReferences.isNotEmpty()) {
            val repaymentWord = DisplayFormatters.pluralize("repayment", BigDecimal(activeRepaymentReferences.size))
            throw LibertyGenericException(
                "Loan ${loanDto.referenceNumber} cannot be voided while it has $repaymentWord: " +
                    activeRepaymentReferences.joinToString()
            )
        }
    }
}
