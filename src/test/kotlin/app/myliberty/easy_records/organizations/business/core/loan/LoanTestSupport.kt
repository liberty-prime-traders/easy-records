package app.myliberty.easy_records.organizations.business.core.loan

import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.model.HasReferenceEntity
import java.time.OffsetDateTime
import java.util.UUID

object LoanTestSupport {


    fun stamp(entity: HasReferenceEntity, reference: String) {
        entity.id = UUID.randomUUID()
        entity.referenceNumber = reference
        entity.createdById = UUID.randomUUID()
        entity.createdOn = OffsetDateTime.now()
    }

    fun lenderContactDto() = ContactDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "CT000001",
        contactTypes = setOf(ContactType.LENDER),
        companyName = "First Bank"
    )
}
