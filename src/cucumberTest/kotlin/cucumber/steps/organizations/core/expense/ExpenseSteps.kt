package app.myliberty.easy_records.cucumber.steps.organizations.core.expense

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExpenseSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val responseContext: ResponseContext
) {

  private val objectMapper = jacksonObjectMapper()
  private var recordedExpenseReference: String? = null

  @Given("a service provider named {string} exists")
  fun createServiceProvider(companyName: String) = createContact(ContactType.SERVICE_PROVIDER, companyName)

  @Given("the expense type {string} is selected")
  fun selectSystemExpenseType(expenseTypeName: String) {
    val expenseTypeListResponse = apiClient.get(EXPENSE_TYPES_PATH)
    check(expenseTypeListResponse.statusCode == 200) {
      "Listing expense types failed: ${expenseTypeListResponse.asString()}"
    }
    val expenseTypeNodes = objectMapper.readTree(expenseTypeListResponse.asString())
    val matchingExpenseTypeNode = expenseTypeNodes.firstOrNull { it.path("name").asText() == expenseTypeName }
    val matchingExpenseTypeId = checkNotNull(matchingExpenseTypeNode) { "No expense type named $expenseTypeName" }
      .path("id").asText()
    injectContext.storeText(TransientKey.SELECTED_EXPENSE_TYPE, matchingExpenseTypeId)
  }

  @Given("a custom expense type named {string} exists")
  fun createCustomExpenseType(expenseTypeName: String) {
    val expenseTypeInsertRequest = mapOf(
      "name" to expenseTypeName,
      "expenseAccountCode" to CUSTOM_EXPENSE_ACCOUNT_CODE,
      "eligiblePayeeTypes" to listOf("SUPPLIER"),
      "eligibleSourceTypes" to listOf("ADHOC")
    )
    val response = apiClient.post(EXPENSE_TYPES_PATH, expenseTypeInsertRequest)
    check(response.statusCode == 200) { "Expense type fixture failed: ${response.statusCode} ${response.asString()}" }
    injectContext.storeText(TransientKey.SELECTED_EXPENSE_TYPE, response.jsonPath().getString("id"))
  }

  @When("I update the selected expense type with payload:")
  fun updateSelectedExpenseType(payload: String) {
    apiClient.put(EXPENSE_TYPES_PATH, withSelectedExpenseType(payload))
  }

  @When("I rename the selected expense type to {string}")
  fun renameSelectedExpenseType(expenseTypeName: String) {
    val expenseTypeRenameRequest = mapOf("id" to selectedExpenseType(), "name" to expenseTypeName)
    apiClient.put("$EXPENSE_TYPES_PATH/name", expenseTypeRenameRequest)
  }

  @When("I record a standalone expense batch with payload:")
  fun recordStandaloneExpenseBatch(payload: String) {
    val response = apiClient.post(STANDALONE_EXPENSES_PATH, withSelectedExpenseType(payload))
    if (response.statusCode == 200) {
      recordedExpenseReference = response.jsonPath().getString("[0].reference")
    }
  }

  @When("I void the recorded expense with reason {string}")
  fun voidRecordedExpense(reason: String) {
    val expenseVoidRequest = mapOf("expenseReference" to recordedExpenseReferenceValue(), "reason" to reason)
    apiClient.post(EXPENSE_VOID_PATH, expenseVoidRequest)
  }

  @When("I get the recorded expense")
  fun getRecordedExpense() {
    apiClient.get("$ORG_EXPENSES_PATH/${recordedExpenseReferenceValue()}")
  }

  @Then("the response error message should be {string} for the recorded expense")
  fun verifyErrorMessageForRecordedExpense(expectedMessage: String) {
    assertNotNull(
      responseContext.lastError,
      "Expected an error response but status was ${responseContext.lastResponse?.statusCode}"
    )
    val expectedMessageWithReference = expectedMessage.replace(RECORDED_EXPENSE_TOKEN, recordedExpenseReferenceValue())
    val actualMessage = responseContext.lastResponse?.jsonPath()?.getString("message")
    assertEquals(expectedMessageWithReference, actualMessage, "Expected error message '$expectedMessageWithReference'")
  }

  private fun withSelectedExpenseType(payload: String): String = injectContext.inject(payload)

  private fun selectedExpenseType(): String =
    checkNotNull(injectContext.findString(TransientKey.SELECTED_EXPENSE_TYPE)) {
      "No expense type has been selected in this scenario"
    }

  private fun recordedExpenseReferenceValue(): String =
    checkNotNull(recordedExpenseReference) { "No expense has been recorded in this scenario" }

  private fun createContact(contactType: ContactType, companyName: String) {
    val contactInsertDto = ContactInsertDto(
      contactTypes = setOf(contactType),
      identityType = IdentityType.ORGANIZATION,
      companyName = companyName
    )
    val response = apiClient.post(CONTACTS_PATH, contactInsertDto)
    check(response.statusCode == 200) { "Failed to create contact '$companyName': ${response.asString()}" }
    injectContext.store(TransientKey.CONTACT, ResponseContext.idFromResponse(response))
    injectContext.storeText(TransientKey.CONTACT_REFERENCE, response.jsonPath().getString("referenceNumber"))
  }

  private companion object {
    const val EXPENSE_TYPES_PATH = "/secured/expense-types"
    const val STANDALONE_EXPENSES_PATH = "/secured/org-expenses/standalone"
    const val EXPENSE_VOID_PATH = "/secured/org-expenses/void"
    const val ORG_EXPENSES_PATH = "/secured/org-expenses"
    const val CONTACTS_PATH = "/secured/contacts"
    const val CUSTOM_EXPENSE_ACCOUNT_CODE = "005.009"
    const val RECORDED_EXPENSE_TOKEN = "{{recordedExpenseReference}}"
  }
}
