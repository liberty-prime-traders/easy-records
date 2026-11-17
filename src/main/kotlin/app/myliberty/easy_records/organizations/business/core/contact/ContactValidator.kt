package app.myliberty.easy_records.organizations.business.core.contact

import app.myliberty.easy_records.organizations.business.core.contact.api.ContactIdentity
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContactValidator(
    private val contactCache: ContactCache
) {

    fun validateContactTypes(types: Set<ContactType>) {
        if(types.isEmpty()) {
            throw RtsGenericException("At least one contact type required")
        }
        if (ContactType.OTHER in types) {
            if(types.size > 1) {
                throw RtsGenericException("OTHER cannot be combined with other contact types")
            }
        }
    }

    fun validateIdentity(
        identityType: IdentityType,
        firstName: String?,
        companyName: String?
    ) {
        when (identityType) {
            IdentityType.ORGANIZATION -> {
                if (companyName.isNullOrBlank()) {
                    throw RtsGenericException("Identity type ORGANIZATION requires company name")
                }
            }
            IdentityType.INDIVIDUAL -> {
                if (firstName.isNullOrBlank()) {
                    throw RtsGenericException("Identity type INDIVIDUAL requires first name")
                }
            }
        }
    }

    fun validateUniquenessAgainst(identity: ContactIdentity, existingNormalizedKeys: Set<String>) {
        if (identity.normalizedKey.isBlank()) return
        if (identity.normalizedKey in existingNormalizedKeys) {
            throw RtsGenericException("A Contact with ${identity.description()} already exists")
        }
    }

    fun validateUniqueness(identity: ContactIdentity, excludeId: UUID? = null) {
        if (identity.normalizedKey.isBlank()) return

        val existingContact = contactCache.getAllContacts()
            .find { contact ->
                contact.id != excludeId && contact.identity.normalizedKey == identity.normalizedKey
            }

        if (existingContact != null) {
            throw RtsGenericException("A Contact with ${identity.description()} already exists")
        }
    }
}
