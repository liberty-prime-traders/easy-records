package app.myliberty.easy_records.organizations.business.core.fund_movement

import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementAccountResponse
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementResponse
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementVoidResponse
import app.myliberty.easy_records.organizations.business.core.account.api.AccountDataFetcher
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component

@Component
class FundMovementResponseBuilder(
    private val accountDataFetcher: AccountDataFetcher,
    private val userQualifier: UserQualifier
) {

    fun buildResponses(fundMovementsWithVoids: List<FundMovementWithVoid>): List<FundMovementResponse> {
        if (fundMovementsWithVoids.isEmpty()) return emptyList()
        val accountCodes = fundMovementsWithVoids
            .flatMap { listOf(it.fundMovementDto.debitAccountCode, it.fundMovementDto.creditAccountCode) }
            .toSet()
        val accountLabelsByCode = accountDataFetcher.getLabelsByCodes(accountCodes)
        return fundMovementsWithVoids.map { toResponse(it.fundMovementDto, accountLabelsByCode, it.fundMovementVoidDto) }
    }

    private fun toResponse(
        fundMovementDto: FundMovementDto,
        accountLabelsByCode: Map<String, String>,
        fundMovementVoidDto: FundMovementVoidDto?
    ) = FundMovementResponse(
        id = fundMovementDto.id,
        referenceNumber = fundMovementDto.referenceNumber,
        type = fundMovementDto.type,
        debitAccount = toAccountResponse(fundMovementDto.debitAccountCode, accountLabelsByCode),
        creditAccount = toAccountResponse(fundMovementDto.creditAccountCode, accountLabelsByCode),
        amount = fundMovementDto.amount,
        movementDate = fundMovementDto.movementDate,
        note = fundMovementDto.note,
        createdOn = fundMovementDto.createdOn,
        createdBy = userQualifier.getCreatorFullName(fundMovementDto.createdById),
        void = fundMovementVoidDto?.let {
            FundMovementVoidResponse(
                voidDate = it.voidDate,
                reason = it.reason,
                voidedBy = userQualifier.getCreatorFullName(it.createdById)
            )
        }
    )

    private fun toAccountResponse(accountCode: String, accountLabelsByCode: Map<String, String>) =
        FundMovementAccountResponse(accountCode, accountLabelsByCode.getValue(accountCode))
}
