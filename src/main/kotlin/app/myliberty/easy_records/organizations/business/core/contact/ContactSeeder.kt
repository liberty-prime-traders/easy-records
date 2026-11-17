package app.myliberty.easy_records.organizations.business.contact

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.ledger.api.SubledgerService
import app.myliberty.easy_records.organizations.business.org_profile.api.OrgDataSeeder
import app.myliberty.easy_records.util.enums.SystemContact
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class ContactSeeder(
    private val contactCache: ContactCache,
    private val contactService: ContactService,
    private val subledgerService: SubledgerService
) : OrgDataSeeder {

    override fun seed() {
        if (contactCache.getAllContacts().none { it.id == SystemContact.WALK_IN.id }) {
            val entity = ContactEntity(
                contactTypes = setOf(ContactType.CUSTOMER),
                firstName = "Walk-In Customer",
                status = ContactStatus.ACTIVE,
                systemDefined = true
            )
            entity.id = SystemContact.WALK_IN.id
            val walkInContact = contactCache.save(entity)
            subledgerService.openBalance(walkInContact.referenceNumber)
        }
        contactService.reconcileSubledgerBalances()
    }
}
