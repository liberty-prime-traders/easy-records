package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.contact.api.IdentityType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContactFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<ContactInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/contacts"

  override fun defaultDto() = ContactInsertDto(
    contactTypes = setOf(ContactType.CUSTOMER),
    identityType = IdentityType.ORGANIZATION,
    companyName = "Test Contact ${UUID.randomUUID()}"
  )
}
