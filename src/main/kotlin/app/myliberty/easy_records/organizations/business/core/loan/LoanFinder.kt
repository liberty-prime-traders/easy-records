package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LoanFinder(
    private val loanRepository: LoanRepository,
    private val loanStatusRepository: LoanStatusRepository,
    private val loanMapper: LoanMapper,
    private val contactService: ContactService,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun lockLoan(loanId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.LOAN, loanId.toString())
    }

    fun findLoan(loanId: UUID): LoanDto = loanMapper.toDto(
        loanRepository.findById(loanId).orElseThrow { RtsGenericException("Loan not found") }
    )

    fun findLoanStatusFigures(loanId: UUID): LoanStatusFigures = loanMapper.toStatusFigures(
        loanStatusRepository.findById(loanId).orElseThrow { RtsGenericException("Loan status not found") }
    )

    fun findLender(loanDto: LoanDto): ContactDto = contactService.getContactById(loanDto.lenderContactId)
}
