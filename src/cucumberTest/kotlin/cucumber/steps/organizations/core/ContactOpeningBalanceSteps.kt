package app.myliberty.easy_records.cucumber.steps.organizations.core

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.rest.JsonSubsetMatcher
import app.myliberty.easy_records.cucumber.support.rest.SubsetOptions
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.response.Response
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ContactOpeningBalanceSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  // Reads amounts as BigDecimal so the history check sees the scale the API prints, not a parsed double.
  private val objectMapper = jacksonObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)

  @Given("an opening balance contact exists")
  fun createActiveOpeningBalanceContact() {
    createOpeningBalanceContact(ContactStatus.ACTIVE)
  }

  @Given("an inactive opening balance contact exists")
  fun createInactiveOpeningBalanceContact() {
    createOpeningBalanceContact(ContactStatus.INACTIVE)
  }

  @Given("a contact with an opening balance of payable {string} and receivable {string}")
  fun createContactWithOpeningBalance(payable: String, receivable: String) {
    createOpeningBalanceContact(ContactStatus.ACTIVE)
    val openingBalanceResponse = postOpeningBalances(payable, receivable)
    check(openingBalanceResponse.statusCode == 200) {
      "Failed to set opening balance: ${openingBalanceResponse.asString()}"
    }
  }

  @When("I set the contact's opening balances to payable {string} and receivable {string}")
  fun setOpeningBalances(payable: String, receivable: String) {
    postOpeningBalances(payable, receivable)
  }

  @Then("the contact's listed net opening balance should be {string} with effect {string}")
  fun verifyListedOpeningBalance(expectedNetOpeningBalance: String, expectedEffect: String) {
    val actualNetOpeningBalance = BigDecimal(listedContactValue("netOpeningBalance"))
    assertEquals(
      0,
      BigDecimal(expectedNetOpeningBalance).compareTo(actualNetOpeningBalance),
      "net opening balance was $actualNetOpeningBalance"
    )
    assertEquals(expectedEffect, listedContactValue("netOpeningBalanceEffect"))
  }

  @Then("the contact's listed balances should be receivable {string} and payable {string}")
  fun verifyListedBalances(expectedReceivable: String, expectedPayable: String) {
    val actualReceivable = BigDecimal(listedContactValue("receivableBalance"))
    val actualPayable = BigDecimal(listedContactValue("payableBalance"))
    assertEquals(
      0,
      BigDecimal(expectedReceivable).compareTo(actualReceivable),
      "listed receivable balance was $actualReceivable"
    )
    assertEquals(
      0,
      BigDecimal(expectedPayable).compareTo(actualPayable),
      "listed payable balance was $actualPayable"
    )
  }

  @Then("the contact's opening balance history should be:")
  fun verifyOpeningBalanceHistory(expectedRevisions: DataTable) {
    val contactReferenceNumber = checkNotNull(injectContext.findString(TransientKey.CONTACT_REFERENCE)) {
      "No contact reference stored for the history check"
    }
    val historyResponse = apiClient.get(
      "/secured/contact-opening-balances/history",
      mapOf("contactReferenceNumber" to contactReferenceNumber)
    )
    val expectedRevisionsJson = objectMapper.valueToTree<JsonNode>(expectedRevisions.asMaps())
    JsonSubsetMatcher.assertJsonSubset(
      expectedRevisionsJson,
      objectMapper.readTree(historyResponse.asString()),
      SubsetOptions(exactLists = true, inOrder = true),
      subject = "opening balance history",
    )
  }

  private fun createOpeningBalanceContact(contactStatus: ContactStatus) {
    val contactInsertDto = ContactInsertDto(
      contactTypes = setOf(ContactType.CUSTOMER),
      identityType = IdentityType.ORGANIZATION,
      companyName = "Opening Balance Contact ${UUID.randomUUID()}",
      status = contactStatus
    )
    val contactResponse = apiClient.post("/secured/contacts", contactInsertDto)
    check(contactResponse.statusCode == 200) { "Failed to create contact: ${contactResponse.asString()}" }
    injectContext.store(TransientKey.CONTACT, ResponseContext.idFromResponse(contactResponse))
    injectContext.storeText(TransientKey.CONTACT_REFERENCE, contactResponse.jsonPath().getString("referenceNumber"))
  }

  private fun postOpeningBalances(payable: String, receivable: String): Response {
    val contactId = injectContext.get(TransientKey.CONTACT)
    return apiClient.post(
      "/secured/contact-opening-balances/$contactId",
      mapOf("payable" to BigDecimal(payable), "receivable" to BigDecimal(receivable))
    )
  }

  private fun listedContactValue(fieldName: String): String {
    val contactId = injectContext.get(TransientKey.CONTACT)
    val listResponse = apiClient.get("/secured/contacts")
    return listResponse.jsonPath().getString("find { it.id == '$contactId' }.$fieldName")
  }
}
