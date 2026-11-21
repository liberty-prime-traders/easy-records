package app.myliberty.easy_records.cucumber.steps.platform

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.support.TestConstants

class AuthenticationSteps(private val authContext: AuthContext) {

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    authContext.authToken = TestConstants.Tokens.ORG_USER
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    authContext.authToken = null
  }
}
