package app.myliberty.easy_records.organizations.business.fund_movement.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementAccountValidator
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementEntity
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementMapper
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementPostingRequests
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementRepository
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementResponseBuilder
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementVoidEntity
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementVoidRepository
import app.myliberty.easy_records.organizations.business.fund_movement.FundMovementWithVoid
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@TransactionalOnOrganizationSchema
class FundMovementService(
    private val fundMovementRepository: FundMovementRepository,
    private val fundMovementVoidRepository: FundMovementVoidRepository,
    private val fundMovementMapper: FundMovementMapper,
    private val fundMovementAccountValidator: FundMovementAccountValidator,
    private val fundMovementResponseBuilder: FundMovementResponseBuilder,
    private val ledgerPostingService: LedgerPostingService,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun createTransfer(fundMovementTransferRequest: FundMovementTransferRequest): FundMovementResponse {
        val fromAccountCode = StringUtils.getValueOrException(fundMovementTransferRequest.fromAccountCode, "From account is required")
        val toAccountCode = StringUtils.getValueOrException(fundMovementTransferRequest.toAccountCode, "To account is required")
        if (fromAccountCode == toAccountCode) {
            throw RtsGenericException("Cannot transfer funds from an account to itself")
        }
        return record(
            FundMovementType.TRANSFER_BETWEEN_ACCOUNTS,
            debitAccountCode = toAccountCode,
            creditAccountCode = fromAccountCode,
            userPickedAccountCodes = listOf(fromAccountCode, toAccountCode),
            amount = fundMovementTransferRequest.amount.toBigDecimal(),
            movementDate = fundMovementTransferRequest.movementDate,
            note = fundMovementTransferRequest.note
        )
    }

    fun createCapital(fundMovementCapitalRequest: FundMovementCapitalRequest): FundMovementResponse {
        val accountCode = StringUtils.getValueOrException(fundMovementCapitalRequest.accountCode, "Account is required")
        return record(
            FundMovementType.CONTRIBUTE_CAPITAL_TO_BUSINESS,
            debitAccountCode = accountCode,
            creditAccountCode = SystemAccount.OWNERS_CAPITAL.code,
            userPickedAccountCodes = listOf(accountCode),
            amount = fundMovementCapitalRequest.amount.toBigDecimal(),
            movementDate = fundMovementCapitalRequest.movementDate,
            note = fundMovementCapitalRequest.note
        )
    }

    fun createDraw(fundMovementDrawRequest: FundMovementDrawRequest): FundMovementResponse {
        val accountCode = StringUtils.getValueOrException(fundMovementDrawRequest.accountCode, "Account is required")
        return record(
            FundMovementType.WITHDRAW_FOR_PERSONAL_USE,
            debitAccountCode = SystemAccount.OWNERS_DRAWS.code,
            creditAccountCode = accountCode,
            userPickedAccountCodes = listOf(accountCode),
            amount = fundMovementDrawRequest.amount.toBigDecimal(),
            movementDate = fundMovementDrawRequest.movementDate,
            note = fundMovementDrawRequest.note
        )
    }

    fun voidFundMovement(fundMovementVoidRequest: FundMovementVoidRequest): FundMovementResponse {
        val reason = StringUtils.getValueOrNull(fundMovementVoidRequest.reason)
        orgEntityAdvisoryLock.acquire(LockNamespaces.FUND_MOVEMENT, fundMovementVoidRequest.id.toString())
        val fundMovementEntity = fundMovementRepository.findById(fundMovementVoidRequest.id)
            .orElseThrow { RtsGenericException("Fund movement not found") }
        if (fundMovementVoidRepository.existsByFundMovementId(fundMovementVoidRequest.id)) {
            throw RtsGenericException("Fund movement ${fundMovementEntity.requiredReference()} is already voided")
        }
        val fundMovementDto = fundMovementMapper.toDto(fundMovementEntity)
        val fundMovementVoidDto = fundMovementMapper.toVoidDto(
            fundMovementVoidRepository.save(
                FundMovementVoidEntity(fundMovementDto.id, DateTimes.Local.Now.organization(), reason)
            )
        )
        ledgerPostingService.post(FundMovementPostingRequests.voided(
                fundMovementDto.referenceNumber, fundMovementDto.debitAccountCode, fundMovementDto.creditAccountCode,
                fundMovementDto.amount, fundMovementVoidDto.voidDate
            ))
        return fundMovementResponseBuilder.buildResponses(listOf(FundMovementWithVoid(fundMovementDto, fundMovementVoidDto))).single()
    }

    private fun record(
        type: FundMovementType,
        debitAccountCode: String,
        creditAccountCode: String,
        userPickedAccountCodes: List<String>,
        amount: BigDecimal,
        movementDate: LocalDate,
        note: String?
    ): FundMovementResponse {
        DateTimes.Local.requireNotInFuture(movementDate, "Movement date")
        val trimmedNote = StringUtils.getValueOrNull(note)
        fundMovementAccountValidator.requireSelectable(userPickedAccountCodes)
        val fundMovementDto = fundMovementMapper.toDto(
            fundMovementRepository.save(
                FundMovementEntity(type, debitAccountCode, creditAccountCode, amount, movementDate, trimmedNote)
            )
        )
        ledgerPostingService.post(FundMovementPostingRequests.recorded(
                fundMovementDto.referenceNumber, fundMovementDto.debitAccountCode, fundMovementDto.creditAccountCode,
                fundMovementDto.amount, fundMovementDto.movementDate
            ))
        return fundMovementResponseBuilder.buildResponses(listOf(FundMovementWithVoid(fundMovementDto, null))).single()
    }
}
