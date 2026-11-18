package app.myliberty.easy_records.organizations.business.core.contact.api

import app.myliberty.easy_records.util.business.Money

/** One row of a bulk contact import. `contactTypes` is a comma-separated list of [ContactType.code]s;
 * the identity type is inferred from [companyName] the same way [ContactInsertDto.identity] does. */
data class ContactBulkInsertDto(
    val contactTypes: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: Money? = null,
    val notes: String? = null
)
