package app.myliberty.easy_records.organizations.business.core.contact.api

import app.myliberty.easy_records.util.business.Money
import java.io.Serializable

data class ContactInsertDto(
    val contactTypes: Set<ContactType>,
    val identityType: IdentityType,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: Money? = null,
    val notes: String? = null,
    val status: ContactStatus = ContactStatus.ACTIVE
): Serializable {
    val identity: ContactIdentity = when (IdentityType.forCompanyName(companyName)) {
        IdentityType.ORGANIZATION -> ContactIdentity.Organization(requireNotNull(companyName))
        IdentityType.INDIVIDUAL -> ContactIdentity.Individual(firstName.orEmpty(), lastName)
    }
}
