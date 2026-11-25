package app.myliberty.easy_records.cucumber.support

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import app.myliberty.easy_records.configuration.security.TenantHeaders
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.support.TestConstants
import org.springframework.core.env.Environment
import org.springframework.core.env.getRequiredProperty
import org.springframework.stereotype.Component

@Component
class AuthenticatedRequestFactory(
  private val environment: Environment,
  private val authContext: AuthContext,
  private val injectContext: InjectContext
) {

  fun jsonRequest(): RequestSpecification {
    val port = environment.getRequiredProperty<Int>("local.server.port")
    val request = given()
      .baseUri("http://localhost:$port")
      .contentType(ContentType.JSON)

    authContext.authToken?.let {
      request.header(TestConstants.Tokens.TOKEN_HEADER, it)
    }
    injectContext.find(PersistentKey.ORGANIZATION)?.let {
      request.header(TenantHeaders.ORGANIZATION_ID_HEADER, it.toString())
    }
    injectContext.find(PersistentKey.LOCATION)?.let {
      request.header(TenantHeaders.LOCATION_ID_HEADER, it.toString())
    }
    return request
  }
}
