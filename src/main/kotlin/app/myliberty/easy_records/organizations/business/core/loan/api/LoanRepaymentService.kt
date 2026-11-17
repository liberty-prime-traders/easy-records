package app.myliberty.easy_records.organizations.business.loan.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.loan.LoanAccountValidator
import app.myliberty.easy_records.organizations.business.loan.LoanFinder
import app.myliberty.easy_records.organizations.business.loan.LoanMapper
import app.myliberty.easy_records.organizations.business.loan.LoanPostingRequests
import app.myliberty.easy_records.organizations.business.loan.LoanRepaymentEntity
import app.myliberty.easy_records.organizations.business.loan.LoanRepaymentRepository
import app.myliberty.easy_records.organizations.business.loan.LoanRepaymentValidator
import app.myliberty.easy_records.organizations.business.loan.LoanRepaymentVoidEntity
import app.myliberty.easy_records.organizations.business.loan.LoanRepaymentVoidRepository
import app.myliberty.easy_records.organizations.business.loan.LoanResponseBuilder
import app.myliberty.easy_records.organizations.business.loan.LoanStatusResolver
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class LoanRepaymentService(
    private val loanRepaymentRepository: LoanRepaymentRepository,
    private val loanRepaymentVoidRepository: LoanRepaymentVoidRepository,
    private val loanMapper: LoanMapper,
    private val loanFinder: LoanFinder,
    private val loanAccountValidator: LoanAccountValidator,
    private val loanRepaymentValidator: LoanRepaymentValidator,
    private val loanResponseBuilder: LoanResponseBuilder,
    private val ledgerPostingService: LedgerPostingService
) {

    fun recordRepayment(loanRepaymentCreateRequest: LoanRepaymentCreateRequest): LoanResponse {
        val repaymentAmount = loanRepaymentCreateRequest.amount.toBigDecimal()
        val assetAccountCode = StringUtils.getValueOrException(loanRepaymentCreateRequest.assetAccountCode, "Asset account is required")
        DateTimes.Local.requireNotInFuture(loanRepaymentCreateRequest.repaymentDate, "Repayment date")
        val trimmedNote = StringUtils.getValueOrNull(loanRepaymentCreateRequest.note)
        loanFinder.lockLoan(loanRepaymentCreateRequest.loanId)
        val loanDto = loanFinder.findLoan(loanRepaymentCreateRequest.loanId)
        val loanStatusFiguresBeforeRepayment = loanFinder.findLoanStatusFigures(loanDto.id)
        loanRepaymentValidator.requireRepayable(
            loanDto, loanStatusFiguresBeforeRepayment, loanRepaymentCreateRequest.repaymentDate, repaymentAmount
        )
        val lenderContactDto = loanFinder.findLender(loanDto)
        loanAccountValidator.requireSelectable(assetAccountCode)
        val loanRepaymentDto = loanMapper.toRepaymentDto(
            loanRepaymentRepository.save(
                LoanRepaymentEntity(loanDto.id, assetAccountCode, repaymentAmount, loanRepaymentCreateRequest.repaymentDate, trimmedNote)
            )
        )
        ledgerPostingService.post(
            LoanPostingRequests.repaymentRecorded(
                loanRepaymentDto.referenceNumber, loanRepaymentDto.assetAccountCode, lenderContactDto.referenceNumber,
                loanRepaymentDto.amount, loanRepaymentDto.repaymentDate
            )
        )
        return loanResponseBuilder.buildResponse(
            loanDto, lenderContactDto, LoanStatusResolver.afterRepayment(loanStatusFiguresBeforeRepayment, loanRepaymentDto.amount)
        )
    }

    fun voidRepayment(loanRepaymentVoidRequest: LoanRepaymentVoidRequest): LoanResponse {
        val reason = StringUtils.getValueOrNull(loanRepaymentVoidRequest.reason)
        val loanRepaymentDto = loanMapper.toRepaymentDto(
            loanRepaymentRepository.findById(loanRepaymentVoidRequest.repaymentId)
                .orElseThrow { RtsGenericException("Loan repayment not found") }
        )
        loanFinder.lockLoan(loanRepaymentDto.loanId)
        loanRepaymentValidator.requireVoidable(loanRepaymentDto)
        val loanDto = loanFinder.findLoan(loanRepaymentDto.loanId)
        val lenderContactDto = loanFinder.findLender(loanDto)
        val loanStatusFiguresBeforeVoid = loanFinder.findLoanStatusFigures(loanDto.id)
        val loanRepaymentVoidDto = loanMapper.toRepaymentVoidDto(
            loanRepaymentVoidRepository.save(
                LoanRepaymentVoidEntity(loanRepaymentDto.id, DateTimes.Local.Now.organization(), reason)
            )
        )
        ledgerPostingService.post(
            LoanPostingRequests.repaymentVoided(
                loanRepaymentDto.referenceNumber, loanRepaymentDto.assetAccountCode, lenderContactDto.referenceNumber,
                loanRepaymentDto.amount, loanRepaymentVoidDto.voidDate
            )
        )
        return loanResponseBuilder.buildResponse(
            loanDto, lenderContactDto, LoanStatusResolver.afterRepaymentVoid(loanStatusFiguresBeforeVoid, loanRepaymentDto.amount)
        )
    }
}
