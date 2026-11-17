package app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningBalanceRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerBalanceDto
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class ContactOpeningBalanceReader(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository
) {

    fun getOpeningBalances(contactReferenceNumbers: Set<String>): Map<String, SubledgerBalanceDto> {
        if (contactReferenceNumbers.isEmpty()) return emptyMap()
        return contactOpeningBalanceRepository.findLatestForContacts(contactReferenceNumbers)
            .groupBy { it.contactReferenceNumber }
            .mapValues { (_, latestRevisions) ->
                val amountsBySubledgerDirection = latestRevisions.associate { it.subledgerDirection to it.amount }
                SubledgerBalanceDto(
                    payable = amountsBySubledgerDirection[ContactSubledgerDirection.PAYABLE] ?: BigDecimal.ZERO,
                    receivable = amountsBySubledgerDirection[ContactSubledgerDirection.RECEIVABLE] ?: BigDecimal.ZERO
                )
            }
    }
}
