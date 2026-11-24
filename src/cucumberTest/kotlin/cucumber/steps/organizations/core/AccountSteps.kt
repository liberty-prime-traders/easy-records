package app.myliberty.easy_records.cucumber.steps.organizations.core

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import app.myliberty.easy_records.cucumber.support.ApiClient
import kotlin.test.assertEquals

class AccountSteps(
  private val apiClient: ApiClient
) {

  @Given("a root account named {string} of type {string} exists")
  fun createRootAccount(accountName: String, accountType: String) {
    val rootCreateRequest = mapOf("name" to accountName, "accountType" to accountType)
    val rootCreateResponse = apiClient.post("/secured/accounts/root", rootCreateRequest)
    assertEquals(200, rootCreateResponse.statusCode, "Failed to create root account: ${rootCreateResponse.asString()}")
  }

  @Given("a child account named {string} exists under {string}")
  fun createChildAccount(accountName: String, parentAccountCode: String) {
    val childCreateRequest = mapOf("parentAccountCode" to parentAccountCode, "name" to accountName)
    val childCreateResponse = apiClient.post("/secured/accounts/child", childCreateRequest)
    val childCreateFailure = "Failed to create child account: ${childCreateResponse.asString()}"
    assertEquals(200, childCreateResponse.statusCode, childCreateFailure)
  }

  @When("I deactivate the account with code {string}")
  fun deactivateAccountWithCode(accountCode: String) {
    val accountId = accountIdForCode(accountCode)
    apiClient.put("/secured/accounts/$accountId/deactivate")
  }

  @When("I activate the account with code {string}")
  fun activateAccountWithCode(accountCode: String) {
    val accountId = accountIdForCode(accountCode)
    apiClient.put("/secured/accounts/$accountId/activate")
  }

  @When("I rename the account with code {string} to {string}")
  fun renameAccountWithCode(accountCode: String, newAccountName: String) {
    val accountId = accountIdForCode(accountCode)
    apiClient.put("/secured/accounts", mapOf("id" to accountId, "name" to newAccountName))
  }

  private fun accountIdForCode(accountCode: String): String {
    val accountsResponse = apiClient.get("/secured/accounts")
    val accountIdJsonPath = "find { it.code == '$accountCode' }.id"
    return checkNotNull(accountsResponse.jsonPath().getString(accountIdJsonPath)) {
      "No account with code $accountCode in ${accountsResponse.asString()}"
    }
  }
}
