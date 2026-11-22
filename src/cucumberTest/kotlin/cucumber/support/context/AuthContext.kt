package app.myliberty.easy_records.cucumber.support.context

import app.myliberty.easy_records.support.TestConstants
import org.springframework.stereotype.Component

@Component
class AuthContext {
  var authToken: String? = null

  fun initialize() {
    authToken = TestConstants.Tokens.ORG_USER
  }

  fun authenticateAsPlatformAdmin() {
    authToken = TestConstants.Tokens.PLATFORM_ADMIN
  }
}
