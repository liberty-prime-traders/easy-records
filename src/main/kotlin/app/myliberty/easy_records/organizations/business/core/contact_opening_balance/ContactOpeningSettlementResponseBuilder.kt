package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementResponse
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementSummaryResponse
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementVoidResponse
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.organizations.business.core.account.api.AccountService
import app.myliberty.easy_records.util.business.MoneyDisplay
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component

@Component
class ContactOpeningSettlementResponseBuilder(
    private val contactOpeningSettlementRepository: ContactOpeningSettlementRepository,
    private val contactOpeningSettlementVoidRepository: ContactOpeningSettlementVoidRepository,
    private val contactOpeningSettlementFinder: ContactOpeningSettlementFinder,
    private val accountService: AccountService,
    private val userQualifier: UserQualifier
) {

    fun buildSummary(contactReferenceNumber: String): ContactOpeningSettlementSummaryResponse {
        val settlementEntities = contactOpeningSettlementRepository
            .findByContactReferenceNumberOrderByCreatedOnDescIdDesc(contactReferenceNumber)
        val voidEntitiesBySettlementId = contactOpeningSettlementVoidRepository
            .findBySettlementIdIn(settlementEntities.map { it.requiredId() })
            .associateBy { it.settlementId }
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return ContactOpeningSettlementSummaryResponse(
            contactReferenceNumber = contactReferenceNumber,
            receivableOutstanding = MoneyDisplay.of(
                contactOpeningSettlementFinder
                    .outstandingAmount(contactReferenceNumber, ContactSubledgerDirection.RECEIVABLE)
            ),
            payableOutstanding = MoneyDisplay.of(
                contactOpeningSettlementFinder
                    .outstandingAmount(contactReferenceNumber, ContactSubledgerDirection.PAYABLE)
            ),
            settlements = settlementEntities.map { settlementEntity ->
                ContactOpeningSettlementResponse(
                    id = settlementEntity.requiredId(),
                    referenceNumber = settlementEntity.requiredReference(),
                    subledgerDirection = settlementEntity.subledgerDirection,
                    paymentAccountCode = settlementEntity.paymentAccountCode,
                    paymentAccountLabel = accountNamesByCode.getValue(settlementEntity.paymentAccountCode),
                    amount = MoneyDisplay.of(settlementEntity.amount),
                    settlementDate = settlementEntity.settlementDate,
                    note = settlementEntity.note,
                    recordedAt = settlementEntity.requiredCreatedOn(),
                    recordedBy = userQualifier.getUserFullName(settlementEntity.createdById) ?: "",
                    void = voidEntitiesBySettlementId[settlementEntity.requiredId()]?.let { voidEntity ->
                        ContactOpeningSettlementVoidResponse(
                            voidDate = voidEntity.voidDate,
                            reason = voidEntity.reason,
                            voidedBy = userQualifier.getUserFullName(voidEntity.createdById) ?: ""
                        )
                    }
                )
            }
        )
    }
}
