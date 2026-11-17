package app.myliberty.easy_records.organizations.business.core.loan.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.loan.LoanAccountValidator
import app.myliberty.easy_records.organizations.business.core.loan.LoanFinder
import app.myliberty.easy_records.organizations.business.core.loan.LoanMapper
import app.myliberty.easy_records.organizations.business.core.loan.LoanPostingRequests
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepaymentEntity
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepaymentRepository
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepaymentValidator
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepaymentVoidEntity
import app.myliberty.easy_records.organizations.business.core.loan.LoanRepaymentVoidRepository
import app.myliberty.easy_records.organizations.business.core.loan.LoanResponseBuilder
import app.myliberty.easy_records.organizations.business.core.loan.LoanStatusResolver
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
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
                .orElseThrow { LibertyGenericException("Loan repayment not found") }
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
