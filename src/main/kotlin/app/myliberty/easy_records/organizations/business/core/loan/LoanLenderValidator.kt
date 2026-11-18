package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component

@Component
class LoanLenderValidator(
    private val contactService: ContactService
) {

    fun requireActiveLender(lenderContactReference: String?): ContactDto {
        val referenceNumber = StringUtils.getValueOrException(lenderContactReference, "Lender is required")
        val lenderContactDto = contactService.findContactByReferenceNumber(referenceNumber)
            ?: throw LibertyGenericException("Lender not found: $referenceNumber")
        val displayName = lenderContactDto.identity.displayName
        if (lenderContactDto.status != ContactStatus.ACTIVE) {
            throw LibertyGenericException("Lender $displayName is not active")
        }
        if (ContactType.LENDER !in lenderContactDto.contactTypes) {
            throw LibertyGenericException("Contact $displayName is not a lender")
        }
        return lenderContactDto
    }
}
