package app.myliberty.easy_records.cucumber.steps.rest

import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import io.cucumber.java.en.Then

class ResponseMemorySteps(
  private val responseContext: ResponseContext,
  private val injectContext: InjectContext
) {

  @Then("I remember the response field {string} as {string}")
  fun rememberResponseField(jsonPath: String, keyName: String) {
    val lastResponse = checkNotNull(responseContext.lastResponse) { "No response to remember '$jsonPath' from" }
    val fieldValue = lastResponse.jsonPath().getString(jsonPath)
    checkNotNull(fieldValue) { "Response has no '$jsonPath': ${lastResponse.asString()}" }
    injectContext.rememberText(keyName, fieldValue)
  }
}
