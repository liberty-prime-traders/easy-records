package app.myliberty.easy_records.cucumber.steps.locations.retail.purchase

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import io.cucumber.java.en.Given

class SupplierSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @Given("a supplier named {string} exists")
  fun createSupplier(companyName: String) {
    val supplierInsertDto = ContactInsertDto(
      contactTypes = setOf(ContactType.SUPPLIER),
      identityType = IdentityType.ORGANIZATION,
      companyName = companyName
    )
    val response = apiClient.post("/secured/contacts", supplierInsertDto)
    check(response.statusCode == 200) { "Failed to create supplier '$companyName': ${response.asString()}" }
    val supplierId = ResponseContext.idFromResponse(response)
    val supplierReference = response.jsonPath().getString("referenceNumber")
    injectContext.store(TransientKey.SUPPLIER, supplierId)
    injectContext.storeText(TransientKey.SUPPLIER_REFERENCE, supplierReference)
    injectContext.storeNamed(TransientKey.SUPPLIER, companyName, supplierId)
    injectContext.storeNamedText(TransientKey.SUPPLIER_REFERENCE, companyName, supplierReference)
  }
}
